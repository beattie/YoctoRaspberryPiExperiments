SUMMARY = "recipe for sshserver image"
DESCRIPTION = "sshserver image"
LICENSE = "MIT"
require recipes-core/images/core-image-base.bb

# set a default password for root
inherit extrausers
EXTRA_USERS_PARAMS = "\
    usermod -P yocto root; \
"

# Add ssh server
IMAGE_FEATURES += " ssh-server-dropbear"

# Configure timezone data
IMAGE_INSTALL += "tzdata"

# Add wifi and bluetooth firmware
IMAGE_INSTALL += "linux-firmware-bcm43430"

# Add Network Manager
IMAGE_INSTALL += "networkmanager networkmanager-bash-completion networkmanager-nmtui"

