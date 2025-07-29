#!/usr/bin/env python3

import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
import os
from pathlib import Path

# Set style for professional plots with university-quality appearance
plt.style.use('default')
plt.rcParams.update({
    'font.size': 12,
    'font.weight': 'bold',
    'axes.titlesize': 18,
    'axes.titleweight': 'bold',
    'axes.labelsize': 14,
    'axes.labelweight': 'bold',
    'xtick.labelsize': 11,
    'ytick.labelsize': 12,
    'legend.fontsize': 12,
    'legend.title_fontsize': 13,
    'figure.titlesize': 20,
    'figure.titleweight': 'bold',
    'lines.linewidth': 3,
    'lines.markersize': 10,
    'grid.alpha': 0.3,
    'grid.linestyle': '--',
    'grid.linewidth': 1,
    'figure.facecolor': 'white',
    'axes.facecolor': '#fafafa',
    'axes.edgecolor': 'gray',
    'axes.linewidth': 1.2,
    'xtick.direction': 'out',
    'ytick.direction': 'out',
    'xtick.major.size': 6,
    'ytick.major.size': 6,
    'legend.frameon': True,
    'legend.fancybox': True,
    'legend.shadow': True
})

# Colorblind-friendly palette with better contrast
COLORS = {
    'sequential': '#D32F2F',    # Darker Red
    'multithreaded': '#388E3C', # Darker Green (changed from parallel)
    'distributed': '#1976D2'    # Darker Blue
}

MARKERS = {
    'sequential': 'o',          # Circle
    'multithreaded': 's',       # Square (changed from parallel)
    'distributed': '^'          # Triangle
}

def load_data():
    """Load and prepare the timing data"""
    try:
        df = pd.read_csv('results/timing_logs/processing_times.csv')
        # Map 'parallel' to 'multithreaded' for consistent naming
        df['mode'] = df['mode'].replace('parallel', 'multithreaded')
        df['pixels'] = df['image_width'] * df['image_height']
        df['megapixels'] = df['pixels'] / 1_000_000
        return df
    except FileNotFoundError:
        print("❌ Error: No timing data found!")
        print("📋 Run ./process_all_images.sh first to generate data")
        return None

def create_output_dir():
    """Create directory for graphs"""
    Path('graphs').mkdir(exist_ok=True)
    return 'graphs'

def analyze_performance(df):
    """Generate comprehensive performance analysis"""
    print("📊 COMPREHENSIVE PERFORMANCE ANALYSIS")
    print("=" * 50)
    
    # Overall averages
    avg_by_mode = df.groupby('mode')['execution_time_ms'].agg(['mean', 'std', 'count'])
    print("\n🏆 OVERALL PERFORMANCE SUMMARY:")
    print("-" * 40)
    for mode, row in avg_by_mode.iterrows():
        mode_display = mode.upper().replace('MULTITHREADED', 'MULTITHREADED')
        print(f"{mode_display:>12}: {row['mean']:8.1f} ms ± {row['std']:6.1f} ms (n={row['count']})")
    
    # Performance by operation
    print("\n🔧 PERFORMANCE BY OPERATION:")
    print("-" * 40)
    op_analysis = df.groupby(['operation', 'mode'])['execution_time_ms'].mean().unstack()
    
    for operation, row in op_analysis.iterrows():
        print(f"\n{operation.upper()}:")
        for mode in ['sequential', 'multithreaded', 'distributed']:
            if mode in row and not pd.isna(row[mode]):
                mode_display = mode.replace('multithreaded', 'multithreaded')
                print(f"  {mode_display:>12}: {row[mode]:8.1f} ms")

def plot_operation_performance(df, operation, output_dir):
    """Create a single clean graph for one operation showing all three modes"""
    # Filter data for the specific operation
    op_data = df[df['operation'] == operation].copy()
    
    if op_data.empty:
        print(f"❌ No data found for operation: {operation}")
        return
    
    # Convert to seconds for better readability
    op_data['execution_time_s'] = op_data['execution_time_ms'] / 1000
    
    # Select key image sizes only (clean presentation)
    key_sizes = [
        (100, 100),     # Very small
        (640, 480),     # Small  
        (1280, 720),    # Medium
        (1920, 1080),   # Large
        (3876, 3999)    # Very large
    ]
    
    # Create the plot
    fig, ax = plt.subplots(figsize=(12, 8))
    
    modes = ['sequential', 'multithreaded', 'distributed']
    mode_labels = ['Sequential', 'Multithreaded', 'Distributed']
    
    # Plot each mode
    for mode, label in zip(modes, mode_labels):
        x_values = []
        y_values = []
        
        # Collect data for key sizes only
        for i, (width, height) in enumerate(key_sizes):
            timing_data = op_data[
                (op_data['image_width'] == width) & 
                (op_data['image_height'] == height) & 
                (op_data['mode'] == mode)
            ]
            
            if not timing_data.empty:
                avg_time = timing_data['execution_time_s'].mean()
                x_values.append(i)  # Use index for even spacing
                y_values.append(avg_time)
        
        if x_values and y_values:
            # Plot the line with markers
            ax.plot(x_values, y_values, 
                   color=COLORS[mode], marker=MARKERS[mode], 
                   linewidth=3, markersize=12, alpha=0.9,
                   label=label, markeredgecolor='white', markeredgewidth=2)
            
            # Add value labels
            for x, y in zip(x_values, y_values):
                ax.annotate(f'{y:.2f}s', 
                           (x, y), 
                           textcoords="offset points", 
                           xytext=(0, 15), 
                           ha='center', va='bottom',
                           fontsize=9, fontweight='bold',
                           bbox=dict(boxstyle='round,pad=0.3', 
                                   facecolor='white', alpha=0.8, edgecolor='gray'))
    
    # Format operation name for title
    operation_title = operation.replace('_', ' ').title()
    if operation == 'edge_detection':
        operation_title = 'Edge Detection'
    
    # Professional styling
    ax.set_title(f'{operation_title} Performance Analysis', 
                fontsize=18, fontweight='bold', pad=20)
    ax.set_xlabel('Image Resolution', fontsize=14, fontweight='bold')
    ax.set_ylabel('Execution Time (seconds)', fontsize=14, fontweight='bold')
    
    # Set custom x-axis labels
    if len(key_sizes) > 0:
        x_positions = list(range(len(key_sizes)))
        x_labels = []
        for width, height in key_sizes:
            pixels = width * height
            megapixels = pixels / 1e6
            if megapixels < 1:
                x_labels.append(f"{width}×{height}\n({megapixels:.2f}MP)")
            else:
                x_labels.append(f"{width}×{height}\n({megapixels:.1f}MP)")
        
        ax.set_xticks(x_positions)
        ax.set_xticklabels(x_labels, fontsize=11)
    
    # Professional legend
    legend = ax.legend(fontsize=12, loc='upper left', frameon=True, 
                      shadow=True, fancybox=True, markerscale=1.2)
    legend.get_frame().set_facecolor('white')
    legend.get_frame().set_alpha(0.95)
    legend.get_frame().set_edgecolor('gray')
    
    # Grid styling
    ax.grid(True, alpha=0.3, linestyle='--', linewidth=1, axis='y')
    ax.set_axisbelow(True)
    
    # Better axis formatting
    ax.yaxis.set_major_formatter(plt.FuncFormatter(lambda x, p: f'{x:.1f}s'))
    
    # Add some padding to the plot
    ax.margins(x=0.05, y=0.1)
    
    # Set background color
    ax.set_facecolor('#fafafa')
    fig.patch.set_facecolor('white')
    
    plt.tight_layout()
    
    # Save with operation-specific name
    filename = f"{operation}_performance"
    plt.savefig(f'{output_dir}/{filename}.png', dpi=300, bbox_inches='tight', 
                facecolor='white', edgecolor='none')
    plt.savefig(f'{output_dir}/{filename}.pdf', bbox_inches='tight',
                facecolor='white', edgecolor='none')
    print(f"📊 Saved: {output_dir}/{filename}.png")
    plt.close()

def plot_speedup_summary(df, output_dir):
    """Create speedup summary graph showing performance improvements"""
    # Calculate speedup data
    speedup_data = []
    
    # Get unique image sizes
    unique_sizes = df.groupby(['image_width', 'image_height']).first().reset_index()
    unique_sizes = unique_sizes.sort_values('pixels')
    
    # Select key sizes for clean presentation
    key_sizes = [
        (100, 100),     # Very small
        (640, 480),     # Small  
        (1280, 720),    # Medium
        (1920, 1080),   # Large
        (3876, 3999)    # Very large
    ]
    
    for width, height in key_sizes:
        size_data = df[(df['image_width'] == width) & (df['image_height'] == height)]
        
        if not size_data.empty:
            # Calculate average times for each mode across all operations
            avg_times = size_data.groupby('mode')['execution_time_ms'].mean()
            
            if 'sequential' in avg_times:
                seq_time = avg_times['sequential']
                
                speedup_row = {
                    'resolution': f"{width}×{height}",
                    'megapixels': (width * height) / 1e6,
                    'sequential_speedup': 1.0,  # Sequential is always baseline (1.0×)
                    'multithreaded_speedup': seq_time / avg_times.get('multithreaded', seq_time) if 'multithreaded' in avg_times else 1.0,
                    'distributed_speedup': seq_time / avg_times.get('distributed', seq_time) if 'distributed' in avg_times else 1.0
                }
                speedup_data.append(speedup_row)
    
    if not speedup_data:
        print("❌ No speedup data available")
        return
    
    speedup_df = pd.DataFrame(speedup_data)
    
    # Create the plot
    fig, ax = plt.subplots(figsize=(14, 8))
    
    x = np.arange(len(speedup_df))
    width = 0.25  # Narrower bars for 3 categories
    
    # Create bars for all three methods
    bars1 = ax.bar(x - width, speedup_df['sequential_speedup'], width, 
                   label='Sequential (Baseline)', color=COLORS['sequential'], alpha=0.8,
                   edgecolor='white', linewidth=2)
    bars2 = ax.bar(x, speedup_df['multithreaded_speedup'], width, 
                   label='Multithreaded', color=COLORS['multithreaded'], alpha=0.8,
                   edgecolor='white', linewidth=2)
    bars3 = ax.bar(x + width, speedup_df['distributed_speedup'], width,
                   label='Distributed', color=COLORS['distributed'], alpha=0.8,
                   edgecolor='white', linewidth=2)
    
    # Add horizontal line at y=1 (no speedup)
    ax.axhline(y=1, color='red', linestyle='--', alpha=0.7, linewidth=2, 
               label='Baseline (No Speedup)')
    
    # Add value labels on bars
    for bars in [bars1, bars2, bars3]:
        for bar in bars:
            height = bar.get_height()
            if height >= 1.0:
                label_text = f'{height:.2f}×'
            else:
                # For values < 1.0, show as slower
                slowdown = 1.0 / height
                label_text = f'{slowdown:.2f}× slower'
            
            ax.text(bar.get_x() + bar.get_width()/2., height + 0.05,
                    label_text, ha='center', va='bottom', 
                    fontsize=9, fontweight='bold')
    
    # Professional styling
    ax.set_title('Performance Speedup Comparison\n(Speedup = Sequential_Time ÷ Method_Time)', 
                fontsize=16, fontweight='bold', pad=20)
    ax.set_ylabel('Speedup Factor', fontsize=14, fontweight='bold')
    ax.set_xlabel('Image Resolution', fontsize=14, fontweight='bold')
    ax.set_xticks(x)
    ax.set_xticklabels(speedup_df['resolution'], fontsize=11)
    
    # Add explanation text
    ax.text(0.02, 0.98, 'Speedup Explanation:\n• 1.0× = Same speed as Sequential\n• 2.0× = Twice as fast as Sequential\n• 0.5× = Half the speed (2× slower)\n\nMP = Megapixels (image size)\n• 0.01MP = 100×100 = 10,000 pixels\n• 15.5MP = 3876×3999 = 15.5M pixels', 
            transform=ax.transAxes, fontsize=9, verticalalignment='top',
            bbox=dict(boxstyle='round,pad=0.5', facecolor='lightyellow', alpha=0.8))
    
    # Professional legend
    legend = ax.legend(fontsize=11, loc='upper right', frameon=True, 
                      shadow=True, fancybox=True)
    legend.get_frame().set_facecolor('white')
    legend.get_frame().set_alpha(0.95)
    legend.get_frame().set_edgecolor('gray')
    
    # Grid styling
    ax.grid(axis='y', alpha=0.3, linestyle='--')
    ax.set_axisbelow(True)
    
    # Set background color
    ax.set_facecolor('#fafafa')
    fig.patch.set_facecolor('white')
    
    # Add some padding
    ax.margins(x=0.05, y=0.1)
    
    # Set y-axis to start from 0 for better comparison
    ax.set_ylim(0, max(ax.get_ylim()[1], 2.5))
    
    plt.tight_layout()
    
    # Save speedup graph
    filename = "speedup_summary"
    plt.savefig(f'{output_dir}/{filename}.png', dpi=300, bbox_inches='tight', 
                facecolor='white', edgecolor='none')
    plt.savefig(f'{output_dir}/{filename}.pdf', bbox_inches='tight',
                facecolor='white', edgecolor='none')
    print(f"📊 Saved: {output_dir}/{filename}.png")
    plt.close()

def generate_latex_report(df, output_dir):
    """Generate LaTeX code for including in reports"""
    latex_content = """
% LaTeX code for including performance analysis graphs
% Copy this into your LaTeX document

\\section{Performance Analysis Results}

\\subsection{Edge Detection Performance}
\\begin{figure}[htbp]
    \\centering
    \\includegraphics[width=0.9\\textwidth]{graphs/edge_detection_performance.pdf}
    \\caption{Edge Detection: Performance analysis across different image sizes}
    \\label{fig:edge_detection_performance}
\\end{figure}

\\subsection{Blur Filter Performance}
\\begin{figure}[htbp]
    \\centering
    \\includegraphics[width=0.9\\textwidth]{graphs/blur_performance.pdf}
    \\caption{Blur Filter: Performance analysis across different image sizes}
    \\label{fig:blur_performance}
\\end{figure}

\\subsection{Sharpen Filter Performance}
\\begin{figure}[htbp]
    \\centering
    \\includegraphics[width=0.9\\textwidth]{graphs/sharpen_performance.pdf}
    \\caption{Sharpen Filter: Performance analysis across different image sizes}
    \\label{fig:sharpen_performance}
\\end{figure}

\\subsection{Performance Speedup Summary}
\\begin{figure}[htbp]
    \\centering
    \\includegraphics[width=0.9\\textwidth]{graphs/speedup_summary.pdf}
    \\caption{Performance speedup comparison: Multithreaded and Distributed vs Sequential processing}
    \\label{fig:speedup_summary}
\\end{figure}
"""
    
    with open(f'{output_dir}/latex_figures.tex', 'w') as f:
        f.write(latex_content)
    
    print(f"📝 Saved: {output_dir}/latex_figures.tex")

def main():
    print("🚀 CLEAN OPERATION-SPECIFIC PERFORMANCE ANALYSIS")
    print("=" * 50)
    
    # Load data
    df = load_data()
    if df is None:
        return
    
    # Create output directory
    output_dir = create_output_dir()
    
    # Generate analysis
    analyze_performance(df)
    
    print(f"\n📈 GENERATING CLEAN GRAPHS...")
    print("-" * 40)
    
    # Generate graphs for each operation (one clean graph each)
    operations = ['edge_detection', 'blur', 'sharpen']
    for operation in operations:
        plot_operation_performance(df, operation, output_dir)
    
    # Generate speedup summary
    plot_speedup_summary(df, output_dir)
    
    # Generate LaTeX code
    generate_latex_report(df, output_dir)
    
    print(f"\n✅ ANALYSIS COMPLETE!")
    print("=" * 50)
    print(f"📁 All graphs saved to: {output_dir}/")
    print(f"📊 Generated files:")
    print(f"  📈 Operation-specific graphs:")
    print(f"    • edge_detection_performance.png/.pdf")
    print(f"    • blur_performance.png/.pdf") 
    print(f"    • sharpen_performance.png/.pdf")
    print(f"  📊 Summary graph:")
    print(f"    • speedup_summary.png/.pdf")
    print(f"  📝 latex_figures.tex (LaTeX code)")
    print(f"\n💡 4 clean graphs total - perfect for university reports!")
    print(f"📝 No more overlapping labels or cluttered layouts!")

if __name__ == "__main__":
    main() 