!/#/bash
javac -cp "lib/*" -d bin src/*.kava
java -cp "bin:lib/*" ClassifyVibration
