import os
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import re
import scipy.stats as stats

import warnings
warnings.filterwarnings("ignore") # damit das Terminal lesbar bleibt

def clean_csv(file_name):
    source_fp = open(file_name, 'r')
    target_fp = open('csv_files/' + file_name + '.csv', 'w')
    first_row = True
    for row in source_fp:
        row = re.sub(r'(?<=\d),(?=\d)', '.', row) # changes 2,48% to 2.48%
        if first_row:
            row = 'unix_time,cycles_done,cur_path,paths_total,pending_total,pending_favs,map_size,unique_crashes,unique_hangs,max_depth,execs_per_sec,valid_inputs,invalid_inputs,valid_cov,all_covered_probes,valid_covered_probes\n'
            first_row = False
        target_fp.write(row)

    return 'csv_files/' + file_name + '.csv'

def merge_csv_marwin(file_names, name): # verwende ich aktuell nicht

    data_frames = [pd.read_csv(file_name) for file_name in file_names] # alle dateien einlesen
    data_frames_sorted = sorted(data_frames, key=lambda x: len(x), reverse=True) # sortiert die dataframes nach der länge (absteigend)
    data_frames_cov = [df['all_covered_probes'] for df in data_frames_sorted] # coverage extrahieren
    
    all_dfs = pd.concat(data_frames_cov, axis=1)
    all_dfs.fillna(method='ffill', inplace=True)
    coverage_columns = all_dfs.filter(like='all_covered_probes')
    row_wise_mean = coverage_columns.mean(axis=1)
    all_dfs['row_wise_mean'] = row_wise_mean
    all_dfs['unix_time'] = data_frames[0]['unix_time']

    all_dfs.dropna(subset=['unix_time'], inplace = True)

    all_dfs.to_csv(f'plot_data_{name}.csv')
    return f'plot_data_{name}.csv'

def merge_csv(file_names, column, name, path): # inner join auf der zeit (Es entfallen Messwerte, aber die Messungen sind synchron)
    data_frames1 = [pd.read_csv(file_name) for file_name in file_names] # alle dateien einlesen
    data_frames = sorted(data_frames1, key=lambda x: len(x), reverse=True) # sortiert die dataframes nach der länge (absteigend) (wichtig bei links join)
    for dataframe in data_frames:
        start_time = dataframe['unix_time'][0]
        dataframe['passed_seconds'] = dataframe['unix_time'] - start_time
    data_frames_cov = [df[[column, 'passed_seconds']] for df in data_frames] # coverage extrahieren
    
    for i in range(0, 10):
        data_frames_cov[i]['Index'] = data_frames_cov[i].groupby('passed_seconds').cumcount()

    all_dfs = pd.merge(data_frames_cov[0], data_frames_cov[1], on=['passed_seconds', 'Index'], how='left')
    for i in range(2, 10):
        all_dfs = pd.merge(all_dfs, data_frames_cov[i], on=['passed_seconds', 'Index'], how='left')

    all_dfs = all_dfs.drop('Index', axis=1) # index wieder löschen
    all_dfs.fillna(method='ffill', inplace=True) # fehlende Werte auffüllen

    coverage_columns = all_dfs.filter(like=column)
    row_wise_mean = coverage_columns.mean(axis=1)
    all_dfs['row_wise_mean'] = row_wise_mean

    all_dfs['unix_time'] = all_dfs['passed_seconds'] + data_frames1[0]['unix_time'][0] # unixtime hinzufügen
    
    all_dfs.to_csv(f'{path}plot_data_{name}.csv')
    return f'{path}plot_data_{name}.csv'


# plot
def plot_csv(file_name, column, color, linestyle, linewidth):
    df = pd.read_csv(file_name)
    start_time = df['unix_time'][0]
    df['passed_seconds'] = df['unix_time'] - start_time

    #df['datetime'] = pd.to_datetime(df['passed_seconds'], unit='s', origin='unix')

    x = df['passed_seconds']
    ticks = np.arange(0, max(df['passed_seconds']), (int) ((max(df['passed_seconds'])) / 9))
    #plt.xticks(ticks, labels=[f"{int(df['passed_seconds'][i])}s" for i in ticks])
    plt.xticks(ticks, [str(int(num / 60)) + ":" + str(num % 60) + " min" for num in ticks])
    y = df[column]
    plt.plot(x, y, color = color, linestyle = linestyle, linewidth = linewidth)


# boxplot
def boxplot_csv(file_names_arr, column, labelnames, title, xlabel, ylabel):
    boxplots = []
    for file_names in file_names_arr:
        data_frames = [pd.read_csv(file_name) for file_name in file_names]
        boxplots.append([df[column].iloc[-1] for df in data_frames])

    fig, ax = plt.subplots()
    ax.boxplot(boxplots)
    plt.xticks(np.arange(1, len(file_names_arr)+1), labelnames)
    ax.set_title(title)
    ax.set_xlabel(xlabel)
    ax.set_ylabel(ylabel)

def mann_whitney_u_test(file_names_complete, file_names_baseline, column, alpha, fuzzername_1, fuzzername_2):
    data_frames_complete = [pd.read_csv(file_name) for file_name in file_names_complete] # einlesen
    data_frames_baseline = [pd.read_csv(file_name) for file_name in file_names_baseline]
    coverage_complete = [dataframe[column].iloc[-1] for dataframe in data_frames_complete] # letzten Wert bestimmen
    coverage_baseline = [dataframe[column].iloc[-1] for dataframe in data_frames_baseline]
    print('----------------------------------------------')
    print('Mann-Whitney-U Test')
    print(f'{fuzzername_1}: {np.mean(coverage_complete)} vs. {fuzzername_2}: {np.mean(coverage_baseline)}')
    significant = stats.mannwhitneyu(coverage_complete, coverage_baseline).pvalue < alpha
    print(f'p < {alpha}: {significant}')
    print(f'p = {stats.mannwhitneyu(coverage_complete, coverage_baseline).pvalue}')
    print('----------------------------------------------')


def init_plot(title, xlabel, ylabel):
    plt.figure(figsize=(10, 6))
    plt.title(title)
    plt.xlabel(xlabel)
    plt.ylabel(ylabel)
    plt.grid(True)

# create new folders
if not os.path.exists('./csv_files'):
    os.makedirs('./csv_files/plot_data_baseline')
    os.makedirs('./csv_files/plot_data_complete')
    os.makedirs('./csv_files/plot_data_probable')
    os.makedirs('./csv_files/plot_data_random')
    os.makedirs('./evaluation_images')

# create .csv files
file_names_complete = [clean_csv(f'plot_data_complete/plot_data_{i}') for i in range(1, 11)]
file_names_baseline = [clean_csv(f'plot_data_baseline/plot_data_baseline_{i}') for i in range(1, 11)]
file_names_probable = [clean_csv(f'plot_data_probable/plot_data_probable_{i}') for i in range(1, 11)]
file_names_random = [clean_csv(f'plot_data_random/plot_data_random_{i}') for i in range(1, 11)]


# all coverage
init_plot('Fuzzing Log: Single Coverage Brances over Time', 'Passed Time', 'Covered Branches')
for file_name in file_names_complete:
    plot_csv(file_name, 'all_covered_probes', 'blue', 'dashed', 1)
for file_name in file_names_baseline:
    plot_csv(file_name, 'all_covered_probes', 'green', 'dashed', 1)
for file_name in file_names_probable:
    plot_csv(file_name, 'all_covered_probes', 'red', 'dashed', 1)
for file_name in file_names_random:
    plot_csv(file_name, 'all_covered_probes', 'black', 'dashed', 1)
#plt.show()
plt.savefig("./evaluation_images/complete_coverage.png", dpi = 300)

# valid coverage
init_plot('Fuzzing Log: Single Valid Coverage Brances over Time', 'Passed Time', 'Valid Covered Branches')
for file_name in file_names_complete:
    plot_csv(file_name, 'valid_covered_probes', 'blue', 'dashed', 1)
for file_name in file_names_baseline:
    plot_csv(file_name, 'valid_covered_probes', 'green', 'dashed', 1)
for file_name in file_names_probable:
    plot_csv(file_name, 'valid_covered_probes', 'red', 'dashed', 1)
for file_name in file_names_random:
    plot_csv(file_name, 'valid_covered_probes', 'black', 'dashed', 1)
#plt.show()
plt.savefig("./evaluation_images/valid_coverage.png", dpi = 300)

merge_csv(file_names_complete, "all_covered_probes", 'average', 'csv_files/plot_data_complete/')
merge_csv(file_names_baseline, "all_covered_probes", 'average_baseline', 'csv_files/plot_data_baseline/')
merge_csv(file_names_probable, "all_covered_probes", 'average_probable', 'csv_files/plot_data_probable/')
merge_csv(file_names_random, "all_covered_probes", 'average_random', 'csv_files/plot_data_random/')

merge_csv(file_names_complete, "valid_covered_probes", 'average_valid', 'csv_files/plot_data_complete/')
merge_csv(file_names_baseline, "valid_covered_probes", 'average_valid_baseline', 'csv_files/plot_data_baseline/')
merge_csv(file_names_probable, "valid_covered_probes", 'average_valid_probable', 'csv_files/plot_data_probable/')
merge_csv(file_names_random, "valid_covered_probes", 'average_valid_random', 'csv_files/plot_data_random/')

#merge_csv_marwin(file_names_random, "average_random_marwin")

# mein average
init_plot("Mean Coverage", "Passed Time", "Covered Branches")
plot_csv('./csv_files/plot_data_complete/plot_data_average.csv', 'row_wise_mean', 'blue', 'solid', 1)
plot_csv('./csv_files/plot_data_baseline/plot_data_average_baseline.csv', 'row_wise_mean', 'green', 'solid', 1)
plot_csv('./csv_files/plot_data_probable/plot_data_average_probable.csv', 'row_wise_mean', 'red', 'solid', 1)
plot_csv('./csv_files/plot_data_random/plot_data_average_random.csv', 'row_wise_mean', 'black', 'solid', 1)

plot_csv('./csv_files/plot_data_complete/plot_data_average_valid.csv', 'row_wise_mean', 'blue', 'dashed', 1)
plot_csv('./csv_files/plot_data_baseline/plot_data_average_valid_baseline.csv', 'row_wise_mean', 'green', 'dashed', 1)
plot_csv('./csv_files/plot_data_probable/plot_data_average_valid_probable.csv', 'row_wise_mean', 'red', 'dashed', 1)
plot_csv('./csv_files/plot_data_random/plot_data_average_valid_random.csv', 'row_wise_mean', 'black', 'dashed', 1)

# marwin average
#plot_csv('./plot_data_average.csv', 'row_wise_mean', 'black', 'solid', 1)
#plot_csv('./plot_data_baseline_average.csv', 'row_wise_mean', 'black', 'solid', 1)
plt.savefig("./evaluation_images/average_coverage.png", dpi = 300)

# boxplots
boxplot_csv([file_names_complete, file_names_baseline, file_names_probable, file_names_random], 'all_covered_probes', ["Complete", "Simple", "Probable", "Random"], "Final Coverage", "", "Covered Branches")
#plt.show()
plt.savefig("./evaluation_images/boxplot_coverage.png", dpi = 300)

mann_whitney_u_test(file_names_complete, file_names_baseline, 'all_covered_probes', 0.01, "Fuzzer Complete", "Fuzzer Baseline")
mann_whitney_u_test(file_names_complete, file_names_probable, 'all_covered_probes', 0.01, "Fuzzer Complete", "Fuzzer Probable")
mann_whitney_u_test(file_names_complete, file_names_random, 'all_covered_probes', 0.01, "Fuzzer Complete", "Fuzzer Random")
mann_whitney_u_test(file_names_baseline, file_names_probable, 'all_covered_probes', 0.01, "Fuzzer Baseline", "Fuzzer Probable")

plt.show()