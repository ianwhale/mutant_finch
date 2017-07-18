#!/usr/bin/env python3

import os
import sys
import csv
from collections import defaultdict

MAX_FITNESS = "MAX_FITNESS"


#
# Helper class from stackoverflow.
#
class cd:
    """
    Context manager for changing the current working directory
    """
    def __init__(self, newPath):
        self.newPath = os.path.expanduser(newPath)

    def __enter__(self):
        self.savedPath = os.getcwd()
        os.chdir(self.newPath)

    def __exit__(self, etype, value, traceback):
        os.chdir(self.savedPath)

def parse(relpath):
    """
    Parse the log file into a useful dictionary.
    @param relpath, relative path to an ecj log file.
    """
    info = {} ## Keep the per-generation fitness

    with open(os.path.abspath(relpath), 'r') as logptr:
        name = os.path.splitext(os.path.basename(logptr.name))[0]
        generation = 0

        for line in logptr:
            if len(line.split('Best Individual of Run:')) == 2:
                ## We don't want the final log read out.
                break

            split = line.split("Fitness: ")

            if len(split) > 1:
                fitness = float(split[1])
                info[generation] = fitness

                generation += 1

    return info

def header(names):
    """
    Make the header for the csv.
    @param names, list of names of columns (should be seed values)
    """
    return ["Generation"] + ["Seed_" + name for name in names]

if __name__ == "__main__":
    """
    Given a command line output a bunch of ecj out directories, we can
    get the log files and parse them into CSVs.
    """
    info = {} ## Keep some information about all the files.
    names = []

    for i in range(1, len(sys.argv)):
        name = sys.argv[i]

        found = os.path.join(".", sys.argv[i])
        if os.path.isdir(found):
            name = os.path.basename(os.path.normpath(name))

            with cd(found):
                log_name = "ecj" + name + ".log"
                ecj_log_path = os.path.join(".", log_name)

                if os.path.isfile(ecj_log_path):
                    info[name] = parse(os.path.join(".", "ecj" + name + ".log"))
                    names.append(name)

    names.sort(key=lambda name: int(name)) # Assumes directories are all integer seed values...

    with open("fitnesses.csv", "w") as csvptr:
        writer = csv.writer(csvptr)
        writer.writerow(header(names))

        maximums = defaultdict(lambda:float('-inf'))
        generations = int(input("How many generations: "))
        for generation in range(generations):
            row = [generation]
            for name in names:
                try:
                    row.append(info[name][generation])
                    maximums[name] = info[name][generation] if \
                                    info[name][generation] > maximums[name] \
                                    else maximums[name]

                except KeyError:
                    row.append("NULL")

            writer.writerow(row)

        writer.writerow([MAX_FITNESS] + [maximums[name] for name in names])

    print("All done. Check out fitnesses.csv for info about your run.")
