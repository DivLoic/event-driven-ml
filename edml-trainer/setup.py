from setuptools import find_packages
from setuptools import setup

REQUIRED_PACKAGES = [
]

setup(
    name='edml-trip-duration',
    version='0.1',
    author='Giulia Bianchi',
    install_requires=REQUIRED_PACKAGES,
    packages=find_packages(),
    include_package_data=True,
    description='Predict trip duration - NYC taxi public dataset',
    requires=[]
)

