#!/usr/bin/env python

from distutils.core import setup
from setuptools import find_packages

if __name__ == '__main__':

    setup(
        name="taxi_trip_duration",
        version='0.1.0-SNAPSHOT',
        description='',
        author='',
        author_email='',
        packages=find_packages(),
        py_modules=['__init__'],
        zip_safe=True,
        data_files=[('scripts', [])],
        classifiers=[
            'Development Status :: 3 - Alpha',
            'Programming Language :: Python'
        ]
    )