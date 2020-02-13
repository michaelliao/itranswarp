#!/usr/bin/env python3

import os, sys, argparse, subprocess
from os.path import expanduser

basedir = os.path.join(os.path.dirname(os.path.abspath(__file__)))

def run(cmd, cwd=None):
    if cwd is None:
        cwd = basedir
    print('\n\x1b[6;30;47m%s\x1b[0m $ %s' % (cwd, cmd))
    subprocess.call(cmd, cwd=cwd, shell=True)

def main():
    parser = argparse.ArgumentParser(description='Deploy itranswarp.')
    parser.add_argument('--profile', nargs=1, required=True)
    args = parser.parse_args()
    profile = args.profile[0]
    print('set basedir to: %s' % basedir)
    print('deploy profile: %s' % profile)
    run('ansible-playbook --version')
    run('ansible-playbook -i ~/.ansible/hosts playbook.yml --extra-vars "profile=%s"' %  profile)

if __name__ == '__main__':
    main()
