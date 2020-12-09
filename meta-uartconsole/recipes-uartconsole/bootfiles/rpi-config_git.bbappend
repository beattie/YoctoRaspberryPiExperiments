SUMMARY = "Modify boot/config.txt to enable console on UART"

do_after_deploy() {
	echo 'enable_uart=1' >> ${DEPLOYDIR}/bcm2835-bootfiles/config.txt
}

addtask after_deploy before do_build after do_install do_deploy
