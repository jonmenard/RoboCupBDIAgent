cd ./Server
start ./rcssserver.exe
cd ..
timeout 1
cd ./Monitor
start ./rcssmonitor.exe
cd ..
timeout 1
cd ./BDIAgentKrislet
start java -cp .;jason-2.3.jar Krislet -team BDI -asl midfielder.asl
start java -cp .;jason-2.3.jar Krislet -team BDI -asl centreback.asl
cd ..
timeout 1
cd ./OriginalKrislet
start java Krislet -team Krislet
start java Krislet -team Krislet
cd ..