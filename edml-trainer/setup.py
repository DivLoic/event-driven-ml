#!/usr/bin/env python

from distutils.core import setup
from setuptools import find_packages

if __name__ == '__main__':

    setup(
        name="trainer",
        version='0.1.0-SNAPSHOT',
        description='Predict trip duration - NYC taxi public dataset',
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