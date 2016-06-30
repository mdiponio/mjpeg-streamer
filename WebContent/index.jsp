<%@ page language="java" 
         contentType="text/html; charset=UTF-8"
		 pageEncoding="UTF-8" 
		 import="java.time.LocalDateTime,
		 		 java.time.format.DateTimeFormatter,
		 		 au.edu.remotelabs.mjpeg.Authenticator,
		 		 au.edu.remotelabs.mjpeg.StreamerHolder,
		 	     au.edu.remotelabs.mjpeg.StreamerConfig.Stream,
		 	     au.edu.remotelabs.mjpeg.source.SourceStream" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
	<title>M-JPEG Streamer</title>
	<meta charset="utf-8">
	<style type="text/css">
	#container {
		width: 1024px;
		margin: 0 auto;
	}
	
	h1 {
		width: 300px;
		text-align: center;
		margin: 0 auto;
	}
	
	table {
		margin: 0 auto;
		border-collapse: collapse;
	}
	
	th {
		font-weight: bold;
	}
	
	td, th {
		border: 1px solid #000000;
		padding: 5px;
	}
	
	td:last-child {
		width: 400px;
	}
	
	ul {
		margin-left: -15px;
		margin-right: 15px;
	}
	
	img {
		width: 120px;
	}
	
	i {
		font-size: 0.8em;
	}
	
	.error {
		color: red;
	}
	</style>
</head>
<body>
	<div id="container">
		<h1>M-JPEG Streamer</h1>
		<table>
			<thead>
				<tr>
					<th>Stream</th>
					<th>URLs</th>
					<th>Last Acquired</th>
					<th>Enabled</th>
					<th>Reading</th>
					<th>Streaming</th>
					<th>Error</th>
				</tr>
			</thead>
			<tbody>
				<% StreamerHolder holder = StreamerHolder.get();
				   Authenticator auth = holder.getAuthenticator();
				   for (Stream stream : holder.getConfig().getStreams().values()) {
				    	SourceStream source = holder.getStream(stream.name);
				    	
				    	String base = request.getRequestURI();
				    	int fp = base.indexOf("index.jsp");
				    	if (fp > 0) base = base.substring(0, fp);
				    	String url = base + "streams/" + stream.name;
				    	
				    	String options = auth.getPassword(stream) == null ? "" : "?pw=" + auth.getPassword(stream);
				%>
				<tr>
					<td><%= stream.name %></td>
					<td>
						<ul>
							<li><a href="<%= url + ".jpeg" + options %>">JPEG</a></li>
							<li><a href="<%= url + ".mjpg" + options %>">M-JPEG</a></li>
							<li><a href="<%= base + "/websocket.jsp?stream=" + stream.name %>">WebSocket</a></li>
						</ul>
					</td>
					<td>
					<% if (source.getLastFrame() != null) { %>
						<a href="<%= url + ".last" %>"><img src="<%= url + ".last" %>" alt="none"></a>
					<% } else { %>
						<i>None</i>	
					<% } %>
					</td>
					<td><%= !source.isDisabled() %></td>
					<td><%= source.isReading() %></td>
					<td><%= source.numStreams() %></td>
					<td><%= source.isErrored() ? "<span class='error'>" + source.getError() + "</span>" : "<i>None</i>" %>
				</tr>
				<% } %>
			</tbody>
		</table>
		<p>Last updated: <%= LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy kk:mm:ss")) %>
	</div>
</body>
</html>