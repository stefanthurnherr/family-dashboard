# family-dashboard
Dashboard for the whole family shown at home


How To Get Started
======
* A Raspberry Pi with a display, internet connection and power supply
* Use the excellent [chilipie-kiosk](https://github.com/jareware/chilipie-kiosk/) RPi image to boot directly into full-screen Chrome
* Update to latest software using apt-get update/upgrade
* Install docker for the RPi
* Build your webapp and Dockerfile (I followed [this tutorial](https://medium.com/swlh/how-to-run-spring-boot-application-on-raspberry-pi-using-docker-d633e15ffff2))
docker container run -p 8080:8080 --restart unless-stopped bondor/dashboard
