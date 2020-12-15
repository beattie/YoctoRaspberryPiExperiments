SUMMARY = "Add wlan0 to /etc/interfaces"

do_install_append() {
	sed -i 's/auto eth0/auto eth0 wlan0/' ${D}${sysconfdir}/network/interfaces
}
