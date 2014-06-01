find input/benchmarks -maxdepth 1 -mindepth 1 -type d \( ! -name . \) -exec bash -c \
'
echo "";
echo $(basename {});
./run.sh -no_out {}/$(basename {}).mini;
gcc asm/$(basename {}).s;
./a.out < {}/input &> out;
diff out {}/output > diffout;
SIZE=$(du diffout | cut -f 1);
if [  "$SIZE" -gt 0 ]; then
   echo "FAILED";
else
   echo "Passed";
fi;
rm -f out diffout a.out;
' \;
