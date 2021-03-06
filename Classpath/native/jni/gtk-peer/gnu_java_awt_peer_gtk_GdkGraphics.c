/* gdkgraphics.c
   Copyright (C) 1999 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

#include "gtkpeer.h"
#include "gnu_java_awt_peer_gtk_GdkGraphics.h"
#include <gdk/gdkprivate.h>
#include <gdk/gdkx.h>

#define GDK_STABLE_IS_PIXMAP(d) (GDK_IS_PIXMAP(d))

GdkPoint *
translate_points (JNIEnv *env, jintArray xpoints, jintArray ypoints, 
		  jint npoints, jint x_offset, jint y_offset);

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_copyState
  (JNIEnv *env, jobject obj, jobject old)
{
  struct graphics *g, *g_old;

  g = (struct graphics *) malloc (sizeof (struct graphics));
  g_old = (struct graphics *) NSA_GET_PTR (env, old);

  *g = *g_old;

  gdk_threads_enter ();

  g->gc = gdk_gc_new (g->drawable);
  gdk_gc_copy (g->gc, g_old->gc);

  if (GDK_STABLE_IS_PIXMAP (g->drawable))
    gdk_pixmap_ref (g->drawable);
  else /* GDK_IS_WINDOW (g->drawable) */
    gdk_window_ref (g->drawable);

  gdk_colormap_ref (g->cm);

  gdk_threads_leave ();

  NSA_SET_PTR (env, obj, g);
}

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_initState__II
  (JNIEnv *env, jobject obj, jint width, jint height)
{
  struct graphics *g;

  g = (struct graphics *) malloc (sizeof (struct graphics));
  g->x_offset = g->y_offset = 0;

  gdk_threads_enter ();
  g->drawable = (GdkDrawable *) gdk_pixmap_new (NULL, width, height, 
						gdk_rgb_get_visual ()->depth);
  g->cm = gdk_rgb_get_cmap ();
  gdk_colormap_ref (g->cm);
  g->gc = gdk_gc_new (g->drawable);
  gdk_threads_leave ();

  NSA_SET_PTR (env, obj, g);
}

/* copy the native state of the peer (GtkWidget *) to the native state
   of the graphics object */
JNIEXPORT jintArray JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_initState__Lgnu_java_awt_peer_gtk_GtkComponentPeer_2
  (JNIEnv *env, jobject obj, jobject peer)
{
  struct graphics *g = (struct graphics *) malloc (sizeof (struct graphics));
  void *ptr;
  GtkWidget *widget;
  GdkColor color;
  jintArray array;
  jint *rgb;

  ptr = NSA_GET_PTR (env, peer);
  g->x_offset = g->y_offset = 0;

  gdk_threads_enter ();

  widget = GTK_WIDGET (ptr);

  if (GTK_IS_WINDOW (widget))
    {
      g->drawable = find_gtk_layout (widget)->bin_window;
    }
  else if (GTK_IS_LAYOUT (widget))
    {
      g->drawable = (GdkDrawable *) GTK_LAYOUT (widget)->bin_window;
    }
  else
    {
      g->drawable = (GdkDrawable *) widget->window;
    }

  gdk_window_ref (g->drawable);
  g->cm = gtk_widget_get_colormap (widget);
  gdk_colormap_ref (g->cm);
  g->gc = gdk_gc_new (g->drawable);
  gdk_gc_copy (g->gc, widget->style->fg_gc[GTK_STATE_NORMAL]);
  color = widget->style->fg[GTK_STATE_NORMAL];

  gdk_threads_leave ();

  array = (*env)->NewIntArray (env, 3);
  rgb = (*env)->GetIntArrayElements (env, array, NULL);
  rgb[0] = color.red >> 8;
  rgb[1] = color.green >> 8;
  rgb[2] = color.blue >> 8;
  (*env)->ReleaseIntArrayElements (env, array, rgb, 0);

  NSA_SET_PTR (env, obj, g);

  return array;
}

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_dispose
  (JNIEnv *env, jobject obj)
{
  struct graphics *g;

  g = (struct graphics *) NSA_DEL_PTR (env, obj);

  if (!g) return;		/* dispose has been called more than once */
  
  gdk_threads_enter ();
  XFlush (GDK_DISPLAY ());

  gdk_gc_destroy (g->gc);

  if (GDK_STABLE_IS_PIXMAP (g->drawable))
    gdk_pixmap_unref (g->drawable);
  else /* GDK_IS_WINDOW (g->drawable) */
    gdk_window_unref (g->drawable);

  gdk_colormap_unref (g->cm);

  gdk_threads_leave ();

  free (g);
}

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_translateNative
  (JNIEnv *env, jobject obj, jint x, jint y)
{
  struct graphics *g;

  g = (struct graphics *) NSA_GET_PTR (env, obj);

  gdk_threads_enter ();

  g->x_offset += x;
  g->y_offset += y;

  gdk_threads_leave ();
}

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_drawString
  (JNIEnv *env, jobject obj, jstring str, jint x, jint y, 
   jstring fname, jint size)
{
  struct graphics *g;
  const char *cfname, *cstr;
  gchar *xlfd;

  g = (struct graphics *) NSA_GET_PTR (env, obj);
  
  cfname = (*env)->GetStringUTFChars (env, fname, NULL);
  xlfd = g_strdup_printf (cfname, (size * 10));
  (*env)->ReleaseStringUTFChars (env, fname, cfname);

  cstr = (*env)->GetStringUTFChars (env, str, NULL);

  gdk_threads_enter ();
  gdk_draw_string (g->drawable, gdk_font_load (xlfd), g->gc, 
		   x + g->x_offset, y + g->y_offset, cstr);
  gdk_threads_leave ();

  (*env)->ReleaseStringUTFChars (env, str, cstr);
  g_free (xlfd);
}


JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_drawLine
  (JNIEnv *env, jobject obj, jint x, jint y, jint x2, jint y2)
{
  struct graphics *g;

  g = (struct graphics *) NSA_GET_PTR (env, obj);

  gdk_threads_enter ();
  gdk_draw_line (g->drawable, g->gc, 
		 x + g->x_offset, y + g->y_offset, 
		 x2 + g->x_offset, y2 + g->y_offset);
  gdk_threads_leave ();
}

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_fillRect
  (JNIEnv *env, jobject obj, jint x, jint y, jint width, jint height)
{
  struct graphics *g;

  g = (struct graphics *) NSA_GET_PTR (env, obj);

  gdk_threads_enter ();
  gdk_draw_rectangle (g->drawable, g->gc, TRUE, 
		      x + g->x_offset, y + g->y_offset, width, height);
  gdk_threads_leave ();
}

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_drawRect
  (JNIEnv *env, jobject obj, jint x, jint y, jint width, jint height)
{
  struct graphics *g;

  g = (struct graphics *) NSA_GET_PTR (env, obj);

  gdk_threads_enter ();
  gdk_draw_rectangle (g->drawable, g->gc, FALSE, 
		      x + g->x_offset, y + g->y_offset, width, height);
  gdk_threads_leave ();
}

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_copyArea
  (JNIEnv *env, jobject obj, jint x, jint y, 
   jint width, jint height, jint dx, jint dy)
{
  struct graphics *g;

  g = (struct graphics *) NSA_GET_PTR (env, obj);

  gdk_threads_enter ();
  gdk_window_copy_area ((GdkWindow *)g->drawable,
			g->gc,
			x + g->x_offset + dx, y + g->y_offset + dy,
			(GdkWindow *)g->drawable,
			x + g->x_offset, y + g->y_offset,
			width, height);
  gdk_threads_leave ();
}

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_copyPixmap
  (JNIEnv *env, jobject obj, jobject offscreen, 
   jint x, jint y, jint width, jint height)
{
  struct graphics *g1, *g2;

  g1 = (struct graphics *) NSA_GET_PTR (env, obj);
  g2 = (struct graphics *) NSA_GET_PTR (env, offscreen);

  gdk_threads_enter ();
  gdk_window_copy_area ((GdkWindow *)g1->drawable,
			g1->gc,
			x + g1->x_offset, y + g1->y_offset,
			(GdkWindow *)g2->drawable,
			0 + g2->x_offset, 0 + g2->y_offset, 
			width, height);
  gdk_threads_leave ();
}
  




JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_clearRect
  (JNIEnv *env, jobject obj, jint x, jint y, jint width, jint height)
{
  struct graphics *g;

  g = (struct graphics *) NSA_GET_PTR (env, obj);

  gdk_threads_enter ();
  gdk_window_clear_area ((GdkWindow *)g->drawable, 
			 x + g->x_offset, y + g->y_offset, width, height);
  gdk_threads_leave ();
}

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_setFunction
  (JNIEnv *env, jobject obj, jint func)
{
  struct graphics *g;
  g = (struct graphics *) NSA_GET_PTR (env, obj);
  
  gdk_threads_enter ();
  gdk_gc_set_function (g->gc, func);
  gdk_threads_leave ();
}


JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_setFGColor
  (JNIEnv *env, jobject obj, jint red, jint green, jint blue)
{
  GdkColor color;
  struct graphics *g;

  color.red = red << 8;
  color.green = green << 8;
  color.blue = blue << 8;

  g = (struct graphics *) NSA_GET_PTR (env, obj);
  
  gdk_threads_enter ();
  gdk_color_alloc (g->cm, &color);
  gdk_gc_set_foreground (g->gc, &color);
  gdk_threads_leave ();
}

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_drawArc
  (JNIEnv *env, jobject obj, jint x, jint y, jint width, jint height, 
   jint angle1, jint angle2)
{
  struct graphics *g;

  g = (struct graphics *) NSA_GET_PTR (env, obj);

  gdk_threads_enter ();
  gdk_draw_arc (g->drawable, g->gc, FALSE, 
		x + g->x_offset, y + g->y_offset, 
		width, height, angle1 << 6, angle2 << 6);
  gdk_threads_leave ();
}  

GdkPoint *
translate_points (JNIEnv *env, jintArray xpoints, jintArray ypoints, 
		  jint npoints, jint x_offset, jint y_offset)
{
  GdkPoint *points;
  jint *x, *y;
  int i;

  /* allocate one more point than necessary, in case we need to tack
     on an extra due to the semantics of Java polygons. */
  points = g_malloc (sizeof (GdkPoint) * (npoints + 1));
  
  x = (*env)->GetIntArrayElements (env, xpoints, NULL);
  y = (*env)->GetIntArrayElements (env, ypoints, NULL);

  for (i = 0; i < npoints; i++)
    {
      points[i].x = x[i] + x_offset;
      points[i].y = y[i] + y_offset;
    }

  (*env)->ReleaseIntArrayElements (env, xpoints, x, JNI_ABORT);
  (*env)->ReleaseIntArrayElements (env, ypoints, y, JNI_ABORT);

  return points;
}

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_drawPolyline
  (JNIEnv *env, jobject obj, jintArray xpoints, jintArray ypoints, 
   jint npoints)
{
  struct graphics *g;
  GdkPoint *points;

  g = (struct graphics *) NSA_GET_PTR (env, obj);
  points = translate_points (env, xpoints, ypoints, npoints,
			     g->x_offset, g->y_offset);

  gdk_threads_enter ();
  gdk_draw_lines (g->drawable, g->gc, points, npoints);
  gdk_threads_leave ();

  g_free (points);
}

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_drawPolygon
  (JNIEnv *env, jobject obj, jintArray xpoints, jintArray ypoints, 
   jint npoints)
{
  struct graphics *g;
  GdkPoint *points;

  g = (struct graphics *) NSA_GET_PTR (env, obj);
  points = translate_points (env, xpoints, ypoints, npoints,
			     g->x_offset, g->y_offset);

  /* make sure the polygon is closed, per Java semantics.
     if it's not, we close it. */
  if (points[0].x != points[npoints-1].x || points[0].y != points[npoints-1].y)
    points[npoints++] = points[0];

  gdk_threads_enter ();
  gdk_draw_lines (g->drawable, g->gc, points, npoints);
  gdk_threads_leave ();

  g_free (points);
}

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_fillPolygon
  (JNIEnv *env, jobject obj, jintArray xpoints, jintArray ypoints, 
   jint npoints)
{
  struct graphics *g;
  GdkPoint *points;

  g = (struct graphics *) NSA_GET_PTR (env, obj);
  points = translate_points (env, xpoints, ypoints, npoints,
			     g->x_offset, g->y_offset);
  gdk_threads_enter ();
  gdk_draw_polygon (g->drawable, g->gc, TRUE, points, npoints);
  gdk_threads_leave ();

  g_free (points);
}

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_fillArc
  (JNIEnv *env, jobject obj, jint x, jint y, jint width, jint height, 
   jint angle1, jint angle2)
{
  struct graphics *g;

  g = (struct graphics *) NSA_GET_PTR (env, obj);

  gdk_threads_enter ();
  gdk_draw_arc (g->drawable, g->gc, TRUE, 
		x + g->x_offset, y + g->y_offset, 
		width, height, angle1 << 6, angle2 << 6);
  gdk_threads_leave ();
}  

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_drawOval
  (JNIEnv *env, jobject obj, jint x, jint y, jint width, jint height)
{
  struct graphics *g;

  g = (struct graphics *) NSA_GET_PTR (env, obj);

  gdk_threads_enter ();
  gdk_draw_arc (g->drawable, g->gc, FALSE, 
		x + g->x_offset, y + g->y_offset, 
		width, height, 0, 23040);
  gdk_threads_leave ();
}  

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_fillOval
  (JNIEnv *env, jobject obj, jint x, jint y, jint width, jint height)
{
  struct graphics *g;

  g = (struct graphics *) NSA_GET_PTR (env, obj);

  gdk_threads_enter ();
  gdk_draw_arc (g->drawable, g->gc, TRUE, 
		x + g->x_offset, y + g->y_offset, 
		width, height, 0, 23040);
  gdk_threads_leave ();
}  

JNIEXPORT void JNICALL Java_gnu_java_awt_peer_gtk_GdkGraphics_setClipRectangle
  (JNIEnv *env, jobject obj, jint x, jint y, jint width, jint height)
{
  struct graphics *g;
  GdkRectangle rectangle;

  g = (struct graphics *) NSA_GET_PTR (env, obj);

  rectangle.x = x + g->x_offset;
  rectangle.y = y + g->y_offset;
  rectangle.width = width;
  rectangle.height = height;

  gdk_threads_enter ();
  gdk_gc_set_clip_rectangle (g->gc, &rectangle);
  gdk_threads_leave ();
}
