%define name libredblack
%define ver 1.2
%define RELEASE 1
%define rel %{?CUSTOM_RELEASE} %{!?CUSTOM_RELEASE:%RELEASE}

Name: %name
Summary: Library for handling red-black tree searching algorithm
Version: %ver
Release: %rel
Copyright: GPL
Group: System Environment/Libraries
Source: ftp://%name.sourceforge.net/pub/%name/%name-%ver.tar.gz
URL: http://%name.sourceforge.net
Packager: Damian Ivereigh <damian@cisco.com>
Prefix: /usr
BuildRoot:/var/tmp/%name-%ver

%package devel
Summary: Additional files and headers required to compile programs using libredblack
Group: Development/Libraries
Requires: %name = %ver

%description 
This implements the redblack balanced tree algorithm.

%description devel
To develop programs based upon the libredblack library, the system needs to 
have these header and object files available for creating the executables.

%prep
%setup

%build
if [ ! -f configure ]; then
  CFLAGS="$RPM_OPT_FLAGS" ./autogen.sh --prefix=%{_prefix}
else
  CFLAGS="$RPM_OPT_FLAGS" ./configure --prefix=%{_prefix}
fi
make

%install
rm -rf ${RPM_BUILD_ROOT}
make prefix=${RPM_BUILD_ROOT}%{_prefix} install

%clean
rm -rf ${RPM_BUILD_ROOT}

%post -p /sbin/ldconfig

%postun -p /sbin/ldconfig

%files
%defattr(-, root, root)
%{_prefix}/lib/libredblack.so.*

%files devel
%defattr(-, root, root)
%{_prefix}/lib/lib*.so
%{_prefix}/lib/*a
%{_prefix}/include/*
%{_prefix}/man/man3/*

