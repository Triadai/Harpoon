/*
 * $Id: DeviceInfo.c,v 1.1 2000-08-22 20:19:26 bdemsky Exp $
 * Purpose: Information functions for Video4Linux
 * Author: Kazushi Mukaiyama <kazu@arizona.ne.jp>
 */

#include "CaptureHeader.h"

void getCapInfo();
void getChanInfo();
void getMmapInfo();
void getWinInfo();
void getColorInfo();
  
void showCapInfo();
void showChanInfo();
void showMmapInfo();
void showWinInfo();
void showColorInfo();

/* get device capability information */
void getCapInfo() {
	status = ioctl(fd, VIDIOCGCAP, &vd);
	if (status == -1) {
		perror("error ioctl(VIDIOCGCAP)");
		exit(1);
	}
}

/* show device capability information */
void showCapInfo() {
	fprintf(stderr, "vd.name: \"%s\"\n", vd.name);
	fprintf(stderr, "vd.type=0x%08x", vd.type);
	if(vd.type&VID_TYPE_CAPTURE) fprintf(stderr, " CAPTURE");
	if(vd.type&VID_TYPE_TUNER) fprintf(stderr, " TUNER");
	if(vd.type&VID_TYPE_TELETEXT) fprintf(stderr, " TELETEXT");
	if(vd.type&VID_TYPE_OVERLAY) fprintf(stderr, " OVERLAY");
	if(vd.type&VID_TYPE_CHROMAKEY) fprintf(stderr, " CHROMAKEY");
	if(vd.type&VID_TYPE_CLIPPING) fprintf(stderr, " CLIPPING");
	if(vd.type&VID_TYPE_FRAMERAM) fprintf(stderr, " FRAMERAM");
	if(vd.type&VID_TYPE_SCALES) fprintf(stderr, " SCALES");
	if(vd.type&VID_TYPE_MONOCHROME) fprintf(stderr, " MONOCHROME");
	if(vd.type&VID_TYPE_SUBCAPTURE) fprintf(stderr, " SUBCAPTURE");
	fprintf(stderr, "\n");
	fprintf(stderr, "vd.channels=%d\n", vd.channels);
	fprintf(stderr, "vd.audios=%d\n", vd.audios);
	fprintf(stderr, "vd.maxwidth=%d\n", vd.maxwidth);
	fprintf(stderr, "vd.maxheight=%d\n", vd.maxheight);
	fprintf(stderr, "vd.minwidth=%d\n", vd.minwidth);
	fprintf(stderr, "vd.minheight=%d\n", vd.minheight);
	fprintf(stderr, "\n");
}
	
/* get device channel information */
void getChanInfo() {
	for(n=0; n<vd.channels; n++) {
		vc[n].channel=n;
		status = ioctl(fd, VIDIOCGCHAN, &vc[n]);
		if (status == -1) {
			perror("error ioctl(VIDIOCGCHAN)");
			exit(1);
		}
	}
}

/* show device channel information */
void showChanInfo() {
	for(n=0; n<vd.channels; n++) {
		fprintf(stderr, "vc[%d].channel=%d\n", n, vc[n].channel);
		fprintf(stderr, "vc[%d].name=\"%s\"\n", n, vc[n].name);
		fprintf(stderr, "vc[%d].tuners=%d\n", n, vc[n].tuners);
		fprintf(stderr, "vc[%d].flags=0x%08x", n, vc[n].flags);
		if(vc[n].flags&VIDEO_VC_TUNER) fprintf(stderr, " TUNER");
		if(vc[n].flags&VIDEO_VC_AUDIO) fprintf(stderr, " AUDIO");
		fprintf(stderr, "\n");
		fprintf(stderr, "vc[%d].type=0x%08x", n, vc[n].type);
		if(vc[n].type&VIDEO_TYPE_TV) fprintf(stderr, " TV");
		if(vc[n].type&VIDEO_TYPE_CAMERA) fprintf(stderr, " CAMERA");
		fprintf(stderr, "\n");
		fprintf(stderr, "vc[%d].norm=%d\n", n, vc[n].norm);
		fprintf(stderr, "\n");
	}
}

/* get device mmap information */
void getMmapInfo() {
	status = ioctl(fd, VIDIOCGMBUF, &vm);
	if (status == -1) {
		perror("error ioctl(VIDIOCGMBUF)");
		exit(1);
	}
}

/* show device mmap information */
void showMmapInfo() {
	fprintf(stderr, "vm.size=0x%08x\n", vm.size);
	fprintf(stderr, "vm.frames=0x%08x\n", vm.frames);
	for(n=0; n<vm.frames; n++)
		fprintf(stderr, "vm.offsets[%d]=0x%08x\n", n, vm.offsets[n]);
	fprintf(stderr, "\n");
}

/* get device window information */
void getWinInfo() {
	status = ioctl(fd, VIDIOCGWIN, &vm);
	if (status < 0) {
		perror("error ioctl(VIDIOCGWIN)");
		exit(1);
	}
}

/* show device window information */
void showWinInfo() {
	fprintf(stderr, "vw.x=%d\n", vw.x);
	fprintf(stderr, "vw.y=%d\n", vw.y);
	fprintf(stderr, "vw.width=%d\n", vw.width);
	fprintf(stderr, "vw.height=%d\n", vw.height);
	fprintf(stderr, "vw.chromakey=%d\n", vw.chromakey);
	fprintf(stderr, "vw.flags=%d\n", vw.flags);
	fprintf(stderr, "\n");
}

/* get device color information */
void getColorInfo() {
	if (ioctl(fd, VIDIOCGPICT, &vp)) {
		perror("ioctl(VIDIOCSPICT)");
		exit(1);
	}
}

/* show device color information */
void showColorInfo() {
	fprintf(stderr, "vp.brightness=%d\n", vp.brightness);
	fprintf(stderr, "vp.hue=%d\n", vp.hue);
	fprintf(stderr, "vp.colour=%d\n", vp.colour);
	fprintf(stderr, "vp.contrast=%d\n", vp.contrast);
	fprintf(stderr, "vp.whiteness=%d\n", vp.whiteness);
	fprintf(stderr, "vp.depth=%d\n", vp.depth);
	fprintf(stderr, "vp.palette=%d\n", vp.palette);
}

