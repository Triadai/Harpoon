VAR=RCHECK WCHECK RWCHECK BASE

all: $(foreach v,$(VAR),bench-$(v).s bench-$(v))

bench-%.s: bench.c
	gcc -D$* -S -O9 $<
	mv bench.s bench-$*.s
bench-%: bench-%.s
	gcc -o $@ -O9 $<

clean:
	$(RM) bench-*.s
	$(RM) bench-*[A-Z]
