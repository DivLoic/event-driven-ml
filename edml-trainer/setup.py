import os
from distutils.core import setup

from setuptools import find_packages

if __name__ == '__main__':
    with open('requirements.txt') as file:
        required = file.read().splitlines()

    setup(
        name="edml-trainer",
        version=os.getenv('version', 'SNAPSHOT'),
        description='Predict trip duration - NYC taxi public dataset',
        author='gbianchi',
        author_email='gbianchi@xebia.fr',
        packages=find_packages(),
        py_modules=['__init__'],
        install_requires=required,
        zip_safe=True,
        data_files=[
            ('/', ['setup.py', 'requirements.txt']),
            ('/scripts', ['ai-platform-submit.sh'])
        ],
        classifiers=[
            'Development Status :: 3 - Alpha',
            'Programming Language :: Python'
        ]
    )
