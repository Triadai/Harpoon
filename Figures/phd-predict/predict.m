# aX = b
#[ t st w r ] [K1] = [ compress ] 
#[ t st w r ] [K2]   [ jess ]
#    ...      [K3]   [ raytrace ]
#    ...      [K4]     ...
#############################################

a = [4892829,	356368802,	48773055;
     4177,	368190502,	51815161;
     45222742,	802165444,	45917064;
     668,	364097220,	108318799;
     2991,	2217932415,	402885754;
     12017041,	137108559,	49921185];
a = a(:,2:3);
b = [5.52;
     4.59;
     2.82;
     7.8;
     30.7;
     3.49];
a = a([1,2,4,5,6],:);
b = b([1,2,4,5,6],:);
K = a \ b;
a * K;

# just writes is a good predictor: K = 76.34 ns overhead
# including reads (but omitting 209_db to avoid a negative coefficient)
# gives 4.35 ns overhead per read, and 53.11 ns overhead per write

##############################
# lets look at predicting using rate.

a = [29746978.46,	4071206.59;
     26507595.54,	3730393.16;
     23565377.32,	1348914.92;
     29819592.14,	8871318.51;
     212853398.75,	38664659.69;
     20711262.69,	7540964.5];
b = [0.46;
     0.33;
     0.08;
     0.64;
     2.95;
     0.53];

K = a\b;
p1 = a * K;

# This is a very good predictor, although it underpredicts jess and
# overpredicts db

# overhead % = 100 * (2.4 ns/read * reads/s + 63.5 ns/write * writes/s)

a=a(:,2);
K = a\b;

# w/ just write: 76.3ns / write

#################################3

a = [26700325.79	4054333.64	3046652.67	16872.95   548299076.17
     26104153.92	3608662.13	403441.61	121731.03  17381.61
     18187254.41	987483.78	5378122.91	361431.14  53424080225.28
     15090.83   	7982.88 	29804501.31	8863335.63 76297.53
     212850162.38	38663920.83	3236.37 	738.87     95700.52
     12695066.01	5902722.21	8016196.68	1638242.3  83320571.28];
a = a(:,[1,2,3,5]);
b = [2.16
     0.59
     5.81
     3.55
     3.27
     6.89];
K = a\b;
a*K;
##################################3

a = [3046652.67 	16872.95	548299076.17	408416.44
     403441.61  	121731.03	17381.61	300.72
     5378122.91 	361431.14	53424080225.28	1328517.69
     29804501.31	8863335.63	76297.53	54.71
     3236.37    	738.87  	95700.52	287.04
     8016196.68 	1638242.3	83320571.28	1815262.99];
a = a(:,[1,3,4]);

b = [1.84
     0.29
     5.71
     3.55
     0.31
     6.48];
K = a\b;
p2 = a * K;
p1 + p2;

# same NT factors as above, plus
# 119ns per transactional read/s, .019ns per transactional write / byte-seconds
# 3.1us per transaction/s

##############################333
a = [3046652.67	16872.95	548299076.17	22651941.99	408416.44
     403441.61	121731.03	17381.61	7036053.51	300.72
     5378122.91	361431.14	53424080225.28	14534338855.55	1328517.69
     29804501.31 8863335.63	76297.53	12360807864.77	54.71
     3236.37	738.87  	95700.52	246338.45	287.04
     8016196.68	1638242.3	83320571.28	75195321.39	1815262.99];
a = a(:,[1,4,5]);
K = a\b;
p2 = a * K;
p1 + p2;

# in this case we get 92.3ns per transactional read/s
# .066 ns per transactional write/byte-seconds
# 3.2us per transaction/s

#a = a(:,1:2)
#K = a\b
#p2 = a*K
#p1 + p2

a = [3046652.67	16872.95
     403441.61	121731.03
     5378122.91	361431.14
     29804501.31	8863335.63
     3236.37	738.87
     8016196.68	1638242.3];
K=a\b;
p2 = a*K;
p1+p2;

###########################333

a = [408416.44 3046652.67
     300.72 403441.61
     1328517.69 5378122.91
     54.71 29804501.31
     287.04 3236.37
     1815262.99 8016196.68];
b = [0.3
     0.06
     1.47
     0.17
     0.01
     1.39];
K=a\b;
a*K;

# this results in 855.8ns per transaction plus 5.5ns per trans. read

a1=a([1,3,6],1);
b1=b([1,3,6],1);
K=a1\b1;
a(:,1)*K;

# 880ns per transaction

# okay, try predicting overall transaction performance using:
# non-transactional write rate, transaction rate, and transactional write rate.
a=[26700325.79	4054333.64	3046652.67	16872.95	548299076.17	22651941.99	408416.44
   26104153.92	3608662.13	403441.61	121731.03	17381.61	7036053.51	300.72
   18187254.41	987483.78	5378122.91	361431.14	53424080225.28	14534338855.55	1328517.69
   15090.83	7982.88	29804501.31	8863335.63	76297.53	12360807864.77	54.71
   212850162.38	38663920.83	3236.37	738.87	95700.52	246338.45	287.04
   12695066.01	5902722.21	8016196.68	1638242.3	83320571.28	75195321.39	1815262.99];
a=a(:,[2,4,7]);
b=[2.16
   0.59
   5.81
   3.55
   3.27
   6.89];
K=a\b;
a*K;

# this gives a model that say NT writes cost 83ns, T writes cost 390ns,
# and transactions cost 3.5us. (transaction cost looks too high)
tc = a(:,3)*880e-9;
b1 = b - tc;
K=a(:,1:2)\b1;
a(:,1:2)*K + tc;

# with transactions at only 880ns each, then jess, db, and jack come out
# low (.8 vs 2.16, 1.4 vs 5.81, 3.0 vs 6.89)
# NT writes: 107ns, T writes 501ns

r = [0.80020
     0.44608
     1.45551
     4.44223
     4.12363
     3.04779];

# w/ 880ns trans and 76.3ns NT write
tc = ( 880e-9 * a(:,3) ) + (76.3e-9 * a(:,1));
b2 = b - tc;
K=a(:,2)\b2;
a(:,2)*K + tc;

# yields 505ns T write
r = [0.67727
     0.33709
     1.42698
     4.47712
     2.95068
     2.87521 ];

# this isn't a very good prediction.

# okay, looking at transaction rate combined with transactional memory op rate
# (as a proxy for transaction size) to predict NOT

a = [408416.44	3063525.63
     300.72	525172.64
     1328517.69	5739554.05
     54.71	38667836.94
     287.04	3975.24
     1815262.99	9654438.97];
b = [0.3
     0.06
     1.47
     0.17
     0.01
     1.39];
K=a\b;
a*K;

# much better!
# this is 859ns / trans + 4ns per transactional memory op
# (what we really want is transactional method calls)

# okay, repeat previous w/ this new model

# fields:
# 1: NT read rate
# 2: NT write rate
# 3: T read rate
# 4: T write rate
# 5: t*sz / time
# 6: write*sz / time
# 7: trans rate
a=[26700325.79	4054333.64	3046652.67	16872.95	548299076.17	22651941.99	408416.44
   26104153.92	3608662.13	403441.61	121731.03	17381.61	7036053.51	300.72
   18187254.41	987483.78	5378122.91	361431.14	53424080225.28	14534338855.55	1328517.69
   15090.83	7982.88	29804501.31	8863335.63	76297.53	12360807864.77	54.71
   212850162.38	38663920.83	3236.37	738.87	95700.52	246338.45	287.04
   12695066.01	5902722.21	8016196.68	1638242.3	83320571.28	75195321.39	1815262.99];
b=[2.16
   0.59
   5.81
   3.55
   3.27
   6.89];
tc = a(:,7)*859e-9 + (a(:,3)+a(:,4))*4e-9 + a(:,1)*2.4e-9 + a(:,2)*63.5e-9;
b1 = b - tc;
# ignore raytrace and mpegaudio, they're nontrans
K = a([1,3,4,6],[4,6]) \ b1([1,3,4,6],:)
a(:,[4,6])*K + tc

# 123 ns / T write + .26ns / write-byte

# both have poor predictive power

# but maybe nontransactional reads/writes are more expensive now,
# since they've got to copy back trans?
tc = a(:,7)*859e-9 + (a(:,3)+a(:,4))*4e-9;
b1 = b - tc;
K = a(:,[2,4,6]) \ b1;
a(:,[2,4,6])*K + tc;

# NT write: 107ns, T write: 115ns, T write*sz: .26ns