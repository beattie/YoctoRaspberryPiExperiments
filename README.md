# YoctoRaspberryPiExperiments
Yocto Experiments for Raspberry Pi and Hopefully Useful Notes
These are my notes on using Yocto to build images for a Raspberry Pi. I used an online course from [Udemy](https://www.udemy.com/course/yocto-zero-to-hero/) (because it was discounted) and some of this might qualify as course notes. _**I make no specific recomendation regarding this course**_

* **[Yocto Layers Repository](http://layers.openembedded.org/layerindex/branch/master/layers/)**
* [Yocto Current Release Manuals](https://docs.yoctoproject.org/releases.html)
* [Standard Images](https://www.yoctoproject.org/docs/1.8/ref-manual/ref-manual.html#ref-images)
* export **BB_NUMBER_THREADS**=8  # twice the output of _cat /proc/cpuinfo | grep processor | wc -l_
  * My system is a 4 Core i5 with 32GB. ```time bitbake ``` gives:
  *   a real execution time of **73m52.588s** for a **Threads** of **4**
  i   a real execution time of **73m19.041s** for a **Threads** of **6**
  *   a real execution time of **73m27.084s** for a **Threads** of **8**
* **bitbake clean**
  * **```bitbake -c clean <recipe-name>```** Removes all output files for a target from the do_unpack task forward
  * **```bitbake -c cleanall <recipe-name>```** Removes all output files, shared state (sstate) cache, and downloaded source files for a target.

---------------------

## Build minimal Raspberry PI 4 image

Install required packages for building on Ubuntu
```
   $ sudo apt-get install gawk wget git-core diffstat unzip texinfo gcc-multilib build-essential chrpath socat libsdl1.2-dev xterm python
```
Get yocto
```
   # "-b zeus" is for the zeus release branch
   $ git clone -b zeus git://git.yoctoproject.org/poky.git poky-minimal
   $ cd poky-minimal
   $ git clone -b zeus git://git.openembedded.org/meta-openembedded
   $ git clone -b zeus git://git.yoctoproject.org/meta-raspberrypi
   $ . oe-init-build-env
```
edit _conf/bblayer.conf_ add raspberrypi to BBLAYERS

#### updated bblayer.conf:
> **_Note: using added ${LAYER_ROOT} variable_**

```
# POKY_BBLAYERS_CONF_VERSION is increased each time build/conf/bblayers.conf
# changes incompatibly
POKY_BBLAYERS_CONF_VERSION = "2"

BBPATH = "${TOPDIR}"
BBFILES ?= ""
LAYER_ROOT_LONG = "${TOPDIR}/../"
LAYER_ROOT = "${@os.path.abspath('${LAYER_ROOT_LONG}')}"


BBLAYERS ?= " \
  ${LAYER_ROOT}/meta \
  ${LAYER_ROOT}/meta-poky \
  ${LAYER_ROOT}/meta-yocto-bsp \
  ${LAYER_ROOT}/meta-raspberrypi \
  "
```
edit _conf/local.conf_ set **MACHINE** to _raspberrypi4-64_ or which ever machine you wish to build for.

```
   $ bitbake core-image-minimal
```
If successful the output is in tmp/deploy/images/${MACHINE}. Prepare a microSD card(assuming /dev/sdb):
```
   $ sudo parted -s /dev/sdb mklabel msdos mkpart primary fat32 1M 100M mkpart primary ext4 100M 100%
   $ sudo mkfs.vfat /dev/sdb1
   $ sudo mkfs.ext4 /dev/sdb2
   $ sudo fatlabel /dev/sdb1 BOOT
   $ sudo e2label /dev/sdb2 ROOT
```
The microSD partitions need to be mounted:
```
   $ sudo mkdir -p /media/BOOT
   $ sudo mkdir -p /media/ROOT
   $ sudo mount /dev/sdb1 /media/BOOT
   $ sudo mount /dev/sdb2 /media/ROOT
```
Copy BOOT files to microSD:
```
   $ cd tmp/deploy/images/<MACHINE>
   $ sudo cp Image bcm2711-rpi-4-b.dtb /media/BOOT
   $ sudo cp -r bcm2835-bootfiles/* /media/BOOT
   $ cd -
```
Locate overlays, either by ```bitbake -e bcm2835-bootfiles | grep "WORKDIR="``` or by ```find tmp -name overlays```. Mine is __tmp/work/<MACHINE>-poky-linux/bcm2835-bootfiles/20191210-r3/firmware-9d6be5b07e81bdfb9c4b9a560e90fbc7477fdc6e/boot/overlays__
```
   $ cd tmp/work/<MACHINE>-poky-linux/bcm2835-bootfiles/20191210-r3/firmware-9d6be5b07e81bdfb9c4b9a560e90fbc7477fdc6e/boot
   $ sudo cp -r overlays /media/BOOT
   $ cd -
```
Copy rootfs to microSD:
```
   $ cd tmp/deploy/images/<MACHINE>
   $ sudo tar -C /media/ROOT -xf *.rootfs.tar.bz2
```
Enable UART set kernel image, change last line of **/media/BOOT/config.txt** from:
```
arm_64bit=1
```
to
```
kernel=Image
arm_64bit=1
enable_uart=1
```
   _**the kernel=Image**_ line may not be needed.
To enable boot messages on UART console change **/media/BOOT/cmdline.txt** to:
```
dwc_otg.lpm_enable=0 console=serial0,115200 console=tty1 root=/dev/mmcblk0p2 rootfstype=ext4 rootwait
```
Unmount microSD and boot RaspberryPI

### An easier way
After all that getting the image to the microSD can also be done by:
```
   $ sudo dd if=tmp/deploy/images/raspberrypi4-64/core-image-minimal-raspberrypi4-64.rpi-sdimg of=/dev/sdb bs=2M status=progress
   $ sudo parted -s /dev/sdb resizepart 2 100%
   $ sudo resize2fs /dev/sdb1
   $ sudo mount /dev/sdb1
```
Edit **/media/BOOT/cmdline.txt** and **/media/BOOT/config.txt** as above.

-----------
## Create a new layer
First example will add a _bbappend_ to enable a console on the UART.

Starting with the minimal build as above. Currently _zeus_ is the latest branch that works with _meta-raspberrypi_.
```
   $ . oe-init-build-env
   $ bitbake-layers create-layer ../meta-uartconsole --example-recipe-name uartconsole
   $ bitbake-layers add-layer ../meta-uartconsole
   $ rm ../meta-uartconsole/recipes-uartconsole/uartconsole
   $ mkdir ../meta-uartconsole/recipes-uartconsole/bootfiles
```
In _meta-uartconsole/recipes-uartconsole/bootfiles_ create a file _rpi-config_git.bbappend_ with the contents:
```
SUMMARY = "Modify boot/config.txt to enable console on UART"

do_after_deploy() {
	echo 'enable_uart=1' >> ${DEPLOY_DIR_IMAGE}/bcm2835-bootfiles/config.txt
}

addtask after_deploy before do_build after do_install do_deploy
```
Modify line added to conf/bblayers.conf to use **${LAYER_ROOT}**

---------------
## Eclipse (WIP)
Apparently development has stopped on the Eclipse Yocto plugin. Every set of instructions I have found is for Eclipse Luna, I'm running 2020-09 and Luna does not seem to want to run on my build Machine. I'm abandoning this for now.

```
   $ bitbake build-sysroots
   $ bitbake meta-toolchain
```

------
## Misc possibly obsolete notes

## Yocto Github projects

* [agherzan/meta-raspberrypi](https://github.com/agherzan/meta-raspberrypi)
* [gt3389b/Yocto-RaspberryPI](https://github.com/gt3389b/Yocto-RaspberryPI)

## References

* [Yocto](https://www.yoctoproject.org/)

### meta-raspberrypi
* [Welcome to meta-raspberrypi’s documentation!](https://meta-raspberrypi.readthedocs.io/en/latest/index.html)

### general reference

* [Hacking Raspberry PI 4 with YOCTO](https://lancesimms.com/RaspberryPi/HackingRaspberryPi4WithYocto_Introduction.html)
* [Building 64-bit Systems for Raspberry PI 4 from Yocto](https://jumpnowtek.com/rpi/Raspberry-Pi-4-64bit-Systems-with-Yocto.html)
* [Building GNU/Linux Distribution for Raspberry Pi Using the Yocto Project](https://www.instructables.com/Building-GNULinux-Distribution-for-Raspberry-Pi-Us/)
* [Yocto for Raspberry pi 4 B 64 bit](https://ineclabs.com/yocto-for-raspberry-pi-64-bit/)

