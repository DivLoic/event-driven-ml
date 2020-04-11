from distutils.core import setup
from setuptools import find_packages
import os

if __name__ == '__main__':

    setup(
        name="edml-trainer",
        version=os.getenv('version', 'SNAPSHOT'),
        description='Predict trip duration - NYC taxi public dataset',
        author='gbianchi',
        author_email='gbianchi@xebia.fr',
        packages=find_packages(),
        py_modules=['__init__'],
        zip_safe=True,
        data_files=[('/scripts', ['ai-platform-submit.sh'])],
        classifiers=[
            'Development Status :: 3 - Alpha',
            'Programming Language :: Python'
        ]
    )
