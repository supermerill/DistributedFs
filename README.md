# DistributedFs
Distributed filesystem to share documents, videos and files between nas, laptop, tablets, servers and other peers as it was in the local filesystem.

# Is it safe to use?
No. It's in alpha. There are no unit tests. THere are no bench. there are no test scenarios

# How to isntall
- Install winfsp (https://github.com/billziss-gh/winfsp)  
ubuntu: sudo apt-get install libfuse-dev  
windows: install the .msi in the release page.
- execute MainInstall (with java 8.172 ou +)
- got to the folder where you say it to install
- execute MainWindow, and look at the logs

# credit
It use jnrfuse (https://github.com/SerCeMan/jnr-fuse)