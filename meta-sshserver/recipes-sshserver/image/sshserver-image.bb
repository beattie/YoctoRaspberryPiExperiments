SUMMARY = "recipe for sshserver image"
DESCRIPTION = "sshserver image"
LICENSE = "MIT"
include recipes-core/images/core-image-base.bb

# Configure timezone data
IMAGE_INSTALL += "tzdata"

# Add ssh server
IMAGE_FEATURES += " ssh-server-dropbear"

# Add wifi and bluetooth firmware
IMAGE_INSTALL += "linux-firmware-bcm43430"

# Add Network Manager
IMAGE_INSTALL += "networkmanager networkmanager-bash-completion networkmanager-nmtui"

# remove old image
RM_OLD_IMAGE = "1"
