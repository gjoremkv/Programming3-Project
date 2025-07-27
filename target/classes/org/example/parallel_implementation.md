Below is a practical guide—followed by two self-contained code sketches—showing the trade-offs among the three most common “do-it-yourself” options in modern Java for CPU-bound workloads like image convolution.

---

## 1  How to think about parallelising a convolution

| Consideration            | Why it matters                                                           | Design note                                                                                                                                                 |
| ------------------------ | ------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Unit of work**         | Each output pixel is independent once the input image is fixed.          | Split the output image into **rows** or **tiles** so that every task touches a contiguous strip, maximising cache locality and eliminating write conflicts. |
| **Read vs. write**       | Input image is read-only; output image is write-only.                    | If every task writes to a distinct region of `outputImage`, you need **no locks**.                                                                          |
| **Core awareness**       | Good speed-ups come only when thread count ≈ core count.                 | Size your pool with `Runtime.getRuntime().availableProcessors()` or leave it to the *common* pool when that is acceptable.                                  |
| **Overhead vs. control** | Finer-grained tasks → better load balancing but more scheduler overhead. | Chunk rows into groups (e.g. 32–128) or use Fork/Join’s adaptive splitting.                                                                                 |

---

## 2  Choosing an execution model

| Model                                                   | One-liner summary                   | Pros                                                                                                                                            | Cons / Gotchas                                                                                                                                                    |
| ------------------------------------------------------- | ----------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Parallel Stream** (`IntStream.range(...).parallel()`) | The *quick-win*; two lines of code. | *Fastest to write*, reads like Java-8 idiom; uses the Fork/Join common pool (already sized to cores); easy to maintain.                         | Little control over pool or task size; can saturate the common pool and slow other parallel parts; per-pixel work must be big enough to amortise stream overhead. |
| **Explicit ForkJoinPool** (`RecursiveAction`)           | DIY divide-and-conquer.             | Full control over pool size, work splitting and exception handling; *work-stealing* keeps cores busy; avoids interference with the common pool. | Slightly more boilerplate; tuning split threshold required for best performance.                                                                                  |
| **ExecutorService / fixed thread pool**                 | Classic “N-threads, N-chunks”.      | Conceptually simple; familiar API; easy to test.                                                                                                | Coarser load balancing (no work-stealing); must join futures yourself; watch out for accidentally creating *too many* tasks.                                      |

> **Rule of thumb:**
> *Try a parallel stream first*. If profiling shows uneven CPU usage or interference with other parallel parts of your app, move to a dedicated `ForkJoinPool`.

---

## 3  Reference implementations

### 3·1 Quick version with Parallel Streams

```java
public static BufferedImage applyConvolutionParallelStream(
        BufferedImage input, double[][] kernel) {

    int width  = input.getWidth();
    int height = input.getHeight();
    int kw = kernel.length;
    int kh = kernel[0].length;
    int kx = kw / 2, ky = kh / 2;

    var output = new BufferedImage(width, height, input.getType());

    // Outer dimension – rows – goes parallel
    IntStream.range(0, height).parallel().forEach(y -> {
        for (int x = 0; x < width; x++) {
            double r = 0, g = 0, b = 0;
            for (int j = 0; j < kh; j++) {
                int yy = clamp(y + j - ky, 0, height - 1);
                for (int i = 0; i < kw; i++) {
                    int xx = clamp(x + i - kx, 0, width - 1);
                    int rgb = input.getRGB(xx, yy);
                    double w = kernel[i][j];
                    r += ((rgb >> 16) & 0xff) * w;
                    g += ((rgb >>  8) & 0xff) * w;
                    b += ( rgb        & 0xff) * w;
                }
            }
            int out = ((clamp((int) Math.round(r), 0, 255) << 16) |
                       (clamp((int) Math.round(g), 0, 255) <<  8) |
                        clamp((int) Math.round(b), 0, 255));
            output.setRGB(x, y, out);
        }
    });

    return output;
}

private static int clamp(int v, int lo, int hi) {
    return v < lo ? lo : (v > hi ? hi : v);
}
```

**Why it works**

* Each stream element (`y`) touches a full row—no two threads write the same pixel.
* Stream overhead is low because each task does `width × kernelSize²` inner-work.
* Adapts automatically to available cores (common pool).

---

### 3·2 Fine-tuned version with a dedicated ForkJoinPool

```java
public final class ConvolutionProcessor {

    private static final int ROW_THRESHOLD = 64;   // tune me!

    public static BufferedImage applyConvolutionForkJoin(
            BufferedImage input, double[][] kernel) {

        int width  = input.getWidth();
        int height = input.getHeight();
        var output = new BufferedImage(width, height, input.getType());

        ForkJoinPool pool = new ForkJoinPool(
                Runtime.getRuntime().availableProcessors());

        pool.invoke(new ConvolutionTask(
                0, height, input, output, kernel));

        return output;
    }

    private static class ConvolutionTask extends RecursiveAction {

        private final int yStart, yEnd;
        private final BufferedImage in, out;
        private final double[][] k;

        ConvolutionTask(int yStart, int yEnd,
                        BufferedImage in, BufferedImage out,
                        double[][] k) {
            this.yStart = yStart;
            this.yEnd   = yEnd;
            this.in     = in;
            this.out    = out;
            this.k      = k;
        }

        @Override
        protected void compute() {
            if (yEnd - yStart <= ROW_THRESHOLD) {
                convolveStrip();
            } else {                 // split task in half
                int mid = (yStart + yEnd) >>> 1;
                invokeAll(new ConvolutionTask(yStart, mid, in, out, k),
                          new ConvolutionTask(mid,     yEnd, in, out, k));
            }
        }

        private void convolveStrip() {
            int w  = in.getWidth();
            int h  = in.getHeight();
            int kw = k.length, kh = k[0].length;
            int kx = kw / 2,   ky = kh / 2;

            for (int y = yStart; y < yEnd; y++) {
                for (int x = 0; x < w; x++) {
                    double r = 0, g = 0, b = 0;
                    for (int j = 0; j < kh; j++) {
                        int yy = clamp(y + j - ky, 0, h - 1);
                        for (int i = 0; i < kw; i++) {
                            int xx = clamp(x + i - kx, 0, w - 1);
                            int rgb = in.getRGB(xx, yy);
                            double wght = k[i][j];
                            r += ((rgb >> 16) & 0xff) * wght;
                            g += ((rgb >>  8) & 0xff) * wght;
                            b += ( rgb        & 0xff) * wght;
                        }
                    }
                    int rgbOut = ((clamp((int) (r + 0.5), 0, 255) << 16) |
                                  (clamp((int) (g + 0.5), 0, 255) <<  8) |
                                   clamp((int) (b + 0.5), 0, 255));
                    out.setRGB(x, y, rgbOut);
                }
            }
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return (v < lo) ? lo : ((v > hi) ? hi : v);
    }
}
```

**Tuning tips**

* `ROW_THRESHOLD` controls granularity; profile with your kernel size & image sizes.
* You own the pool, so it never starves unrelated parallel stream work elsewhere.
* If you have multiple images to process, keep the pool around and reuse it.

---

## 4  When *not* to build it yourself

* **Very large kernels** or **multiple channels** (e.g. convolutional neural-net layers): consider GPU off-loading (JOCL, Aparapi, TornadoVM, CUDA via JNI).
* **Plenty of well-tested libraries** already do this (OpenIMAJ, Marvin, BoofCV, JavaCV). Rolling your own makes sense for learning, special kernels, or removing dependencies.

---

### Bottom line

* For a quick, maintainable speed-up, wrap your inner loop in a **parallel stream**—often a five-line change.
* When you need dedicated resources, predictable behaviour, or maximum throughput in a server/back-end setting, invest in a **ForkJoinPool** with adaptive row splitting.
* `ExecutorService` is fine when work items are big and fixed (e.g. *one image per thread*), but it gives you less automatic load balancing than Fork/Join.

Happy coding—and don’t forget to profile both versions on realistic images before finalising your choice!
