@echo off
:: костыльный скрипт сборки тестовой дллки WinJFX
if not exist "..\out\winjfx" md "..\out\winjfx"
cd "..\out\winjfx"
set COMPILER="C:\MinGW-w64\mingw64\bin\g++"
%COMPILER% -I"C:\Program Files\Java\jdk1.8.0_202\include" -I"C:\Program Files\Java\jdk1.8.0_202\include\win32" -c "..\..\src\main\native\WinJFX.c"
%COMPILER% -s -shared -o WinJFX.dll WinJFX.o -lole32 -luuid -lgdi32
echo 
pause
