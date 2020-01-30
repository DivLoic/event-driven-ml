from distutils.core import setup
from setuptools import find_packages
import configparser


if __name__ == '__main__':

    with open('../build/version.properties', 'r', encoding="utf-8") as f:
        config_string = '[root]\n' + f.read()

    config = configparser.ConfigParser()
    config.read_string(config_string, 'utf-8')

    setup(
        name="edml-trainer",
        version=config['root']['VERSION_DISPLAY'],
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
