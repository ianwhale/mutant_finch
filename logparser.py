#!/usr/bin/env python3

import os
import sys
import csv

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
    info = {} ## Keep some information about each file.

    with open(os.path.abspath(relpath), 'r') as logptr:
        name = os.path.splitext(os.path.basename(logptr.name))[0]
        generation = 0

        for line in logptr:
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
            with cd(found):
                if os.path.isfile(os.path.join(".", "ecj" + name + ".log")):
                    info[name] = parse(os.path.join(".", "ecj" + name + ".log"))
                    names.append(name)

    with open("fitnesses.csv", "w") as csvptr:
        writer = csv.writer(csvptr)

        writer.writerow(header(names))

        for generation in range(len(info[names[0]])):
            row = [generation]
            for name in names:
                row.append(info[name][generation])

            writer.writerow(row)
