@echo off
echo Starting WhatsApp Clone Application...
echo.
echo Server will start on http://localhost:8081
echo Press Ctrl+C to stop the server
echo.
java -jar target/whatsapp-clone-1.0.0.war --server.port=8081
