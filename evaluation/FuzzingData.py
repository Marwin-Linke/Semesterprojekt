import os

import pandas
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
from IPython.display import display
from datetime import datetime

from matplotlib.ticker import FormatStrFormatter
from scipy.ndimage import gaussian_filter1d


def convert_to_csv(file_name):
    source_fp = open(file_name, 'r')
    target_fp = open(file_name + '.csv', 'w')
    first_row = True
    for row in source_fp:
        if first_row:
            row = 'unix_time,cycles_done,cur_path,paths_total,pending_total,pending_favs,map_size,map_size_decimal,unique_crashes,unique_hangs,max_depth,execs_per_sec,execs_per_sec_decimal,valid_inputs,invalid_inputs,valid_cov,valid_cov_decimal,all_covered_probes,valid_covered_probes\n'
            first_row = False
        target_fp.write(row)

    return file_name + '.csv'

def merge_csv(file_names, name):

    data_frames = [pd.read_csv(file_name) for file_name in file_names]
    data_frames_cov = [df['all_covered_probes'] for df in data_frames]
    all_dfs = pandas.concat(data_frames_cov, axis=1)
    coverage_columns = all_dfs.filter(like='all_covered_probes')
    row_wise_mean = coverage_columns.mean(axis=1)
    all_dfs['row_wise_mean'] = row_wise_mean
    all_dfs['unix_time'] = data_frames[0]['unix_time']

    all_dfs.dropna(subset=['unix_time'], inplace = True)

    all_dfs.to_csv(f'plot_data_{name}.csv')
    return f'plot_data_{name}.csv'

def plot_csv(file_name, column, color, linestyle, linewidth):

    input_file = file_name
    df = pd.read_csv(input_file)

    start_time = df['unix_time'][0]
    df['datetime'] = pd.to_datetime(df['unix_time'] - start_time, unit='s', origin='unix')
    df['passed_seconds'] = df['unix_time'] - start_time

    x = df.index
    ticks = np.arange(min(df.index), max(df.index), size/10)
    plt.xticks(ticks, labels=[f"{int(df['passed_seconds'][i])}s" for i in ticks])
    y = df[column]
    plt.plot(x, y, color = color, linestyle = linestyle, linewidth = linewidth)

def plot_csv_datetime(file_name, column, color, linestyle, linewidth):
    input_file = file_name
    df = pd.read_csv(input_file)

    start_time = df['unix_time'][0]
    df['datetime'] = pd.to_datetime(df['unix_time'] - start_time, unit='s', origin='unix')
    df['hour_minute'] = df['datetime'].dt.strftime('%H:%M')

    # Plotting
    x = df['datetime']
    y = df[column]
    plt.plot(x, y, color=color, linestyle=linestyle, linewidth=linewidth)


def init_plot():
    plt.figure(figsize=(10, 6))

    plt.title('Fuzzing Log: Single Coverage Branches over Time')
    plt.xlabel('Passed Seconds')
    plt.ylabel('Covered Branches')
    plt.grid(True)

size = 200

file_names = [convert_to_csv(f'plot_data_complete/plot_data_{i}') for i in range(1, 11)]
average_plot = merge_csv(file_names, "average")
file_names_baseline = [convert_to_csv(f'plot_data_baseline/plot_data_baseline_{i}') for i in range(1, 11)]
average_plot_baseline = merge_csv(file_names_baseline, "baseline_average")
init_plot()
for file_name in file_names:
    plot_csv(file_name, 'all_covered_probes', 'blue', 'dashed', 1)
#plot_csv(average_plot, 'row_wise_mean', 'black', 'solid', 3)
for file_name in file_names_baseline:
    plot_csv(file_name, 'all_covered_probes', 'green', 'dashed', 1)
#plot_csv(average_plot_baseline, 'row_wise_mean', 'black', 'solid', 3)
plt.xlim(-size/10, size)
#plt.show()

plt.savefig("Single_Coverage_Comparison_60s.png", dpi = 300)
# plt.plot(df['passed_seconds'], df['valid_inputs'] / (df['valid_inputs'] + df['invalid_inputs']) * 1000, linestyle='-', color='g')
