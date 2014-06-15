find input/benchmarks -maxdepth 1 -mindepth 1 -type d \( ! -name . \) -exec bash -c \
'
out=""
loc=""
if [ "$0" == "-q" ]
then
   out="-no_out"
fi

if [ "$0" == "-s" ]
then
   out="-no_out"
   loc=" &> /dev/null"
fi

if [ "$0" == "-t" ]
then
   out="-no_out"
   loc=" &> /dev/null"
fi

echo ""
echo $(basename {})

./run.sh $out {}/$(basename {}).mini $loc
gcc asm/$(basename {}).s

./a.out < {}/input &> out
diff out {}/output > diffout
touch diffout
SIZE=$(du diffout | cut -f 1)

if [  "$SIZE" -gt 0 ]
then
   echo "FAILED"
else
   echo "Passed"

   if [ "$0" == "-t" ]
   then
      echo ""
      echo "Compiled"
      time ./a.out < {}/input &> out
      gcc {}/$(basename {}).c -O0
      echo ""
      echo "O0"
      time ./a.out < {}/input &> out
      gcc {}/$(basename {}).c -O1
      echo ""
      echo "O1"
      time ./a.out < {}/input &> out
      gcc {}/$(basename {}).c -O2
      echo ""
      echo "O2"
      time ./a.out < {}/input &> out
      gcc {}/$(basename {}).c -O3
      echo ""
      echo "O3"
      time ./a.out < {}/input &> out
   fi
fi

rm -f out diffout a.out
' $1 \;
