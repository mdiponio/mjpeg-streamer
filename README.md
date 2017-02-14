# mjpeg-streamer

## Introduction
M-JPEG streaming application which runs on Tomcat 7+ and provides the following functions:

  * Reads a M-JPEG video feed, a format commonly served from a network security cameras.
  * Applies transformations to video feeds such as scaling, rotating, cropping, perspective transformations. These transformations are on-demand, specified as part of feed request URL encoded parameters.
  * Outputs either:
   - Single JPEG frame
   - M-JPEG HTTP feed
   - JPEG frames to a websocket
  * Output feeds may be password protected, started or stopped through a REST API.

## Build
Code compilation and export to WAR package uses Eclipse IDE.
 1. Import project into Eclipse.
 2. Export WAR using Export -> Web/WAR wizard in Eclipse.

## Deployment
  1. Added WAR to Tomcat webapps folder.
  2. Set Servlet init parameter to "streamer-config" to point to an XML configuration file.
  3. Configure XML configuration file.

## Configuration
See example at https://github.com/mdiponio/mjpeg-streamer/blob/master/WebContent/META-INF/streams-config.xml

## Request URL format
 * M-JPEG -  `http://<server>/<app>/streams/<stream>.mjpg?<transform list>`
 * Buffered M-JPEG -  `http://<server>/<app>/streams/<stream>.bjpg?<transform list>` 
 * JPEG - `http://<server>/<app>/streams/<stream>.jpeg?<transform list>`
 * Last acquired frame - `http://<server>/<app>/stream>.last`
 * Websocket - `http://<server/<app/websocket.jsp?stream=<stream>&<transform list>`

Where
 * `<server>` - URL to web server
 * `<app>` - Servlet path to M-JPEG application on Tomcat
 * `<stream>` - Configured name of stream
 * `<transform>` - List of transformations and transform parameters in order of application

## Supported Transformations

 
| Transformation | Parameters | Description |
| -------------- | ---------- | ----------- |
| crop | `<x>,<y>,<width>,<height>` | Crop a region of stream using offset coordinate and size. |
| debarrel | `<strength>,<zoom>` | Apply barrel correction to image, can be used to correct image fish-eye caused by lens distortion. |
| quality | `<percent>` | Reduce output image size, increase JPEG compression. 1 highest compression, lowest quality, 100 lowest compression. |
| size | `<width>x<height>[,keepRatio]` | Scale output image to new width and height, optionally preserving aspect ratio.
| timestamp | N/A | Add a timestamp to the image. |
| rotate | `<angle>[rad][,clip]` | Rotate the image about the image center. Rotation angle is in degrees or radians if 'rad' specified. If the rotation of the image puts regions outside the bounds of the image, the image will be resized, or optionally clipped. |
| perspective | `<m00>,<m01>,<m02>,<m10>,<m11>,<m12>,<m20>,<m21>,<m22>` | Applies a perspective transformation with the specified perspective matrix. |

Multiple transforms can be used and each will be applied in the same sequence as they exist in the request URL. For example `<URL>?size=640x480&rotate=180&timestamp` will first resize source to width 640px and height 480px, then rotate 180 degrees (flip on Y axis) and finally add a timestamp to the top left of image.

## Format Specific Parameters
| Parameter | Arguments | Applies To | Description |
| --------- | --------- | ---------- | ----------- |
| frame_rate (or fr) | `<rate>` | M-Jpeg / Buffered M-Jpeg | Allows frame rate of the returned stream to be set. This rate is specified in terms of frames per second and should be smaller than source stream frame rate to have any effect. For example, if the source frame rate is 25 frames a second and the rate argument is 1, 1 frame a second will be returned and 24 discarded. |
| sequence (or seq) | N/A | M-Jpeg / Buffered M-Jpeg | Adds a sequence number header 'frame-sequence' before each frame is sent, specifying the source stream frame number since the start of the stream. If frames are dropped, the sequence number will not be continuous. Subtracted the current frame sequence from the previously received frame sequence will give the number of frames dropped between the current and previously received frame. |
| wait | `<number>` | Buffered M-Jpeg | Specifies the number of frames that are buffered, if not read fast enough, before frames are discarded. | 