/* 
 * Copyright 1988, 1989 Hans-J. Boehm, Alan J. Demers
 * Copyright (c) 1991-1995 by Xerox Corporation.  All rights reserved.
 * Copyright 1996-1999 by Silicon Graphics.  All rights reserved.
 * Copyright 1999 by Hewlett-Packard Company.  All rights reserved.
 *
 * THIS MATERIAL IS PROVIDED AS IS, WITH ABSOLUTELY NO WARRANTY EXPRESSED
 * OR IMPLIED.  ANY USE IS AT YOUR OWN RISK.
 *
 * Permission is hereby granted to use or copy this program
 * for any purpose,  provided the above notices are retained on all copies.
 * Permission to modify the code and to distribute modified code is granted,
 * provided the above notices are retained, and a notice that the code was
 * modified is included with the above copyright notice.
 */

/*
 * Note that this defines a large number of tuning hooks, which can
 * safely be ignored in nearly all cases.  For normal use it suffices
 * to call only GC_MALLOC and perhaps GC_REALLOC.
 * For better performance, also look at GC_MALLOC_ATOMIC, and
 * GC_enable_incremental.  If you need an action to be performed
 * immediately before an object is collected, look at GC_register_finalizer.
 * If you are using Solaris threads, look at the end of this file.
 * Everything else is best ignored unless you encounter performance
 * problems.
 */
 
#ifndef _GC_H

# define _GC_H

# include "gc_config_macros.h"

# if defined(__STDC__) || defined(__cplusplus)
#   define GC_PROTO(args) args
    typedef void * GC_PTR;
#   define GC_CONST const
# else
#   define GC_PROTO(args) ()
    typedef char * GC_PTR;
#   define GC_CONST
#  endif

# ifdef __cplusplus
    extern "C" {
# endif


/* Define word and signed_word to be unsigned and signed types of the 	*/
/* size as char * or void *.  There seems to be no way to do this	*/
/* even semi-portably.  The following is probably no better/worse 	*/
/* than almost anything else.						*/
/* The ANSI standard suggests that size_t and ptr_diff_t might be 	*/
/* better choices.  But those had incorrect definitions on some older	*/
/* systems.  Notably "typedef int size_t" is WRONG.			*/
#ifndef _WIN64
  typedef unsigned long GC_word;
  typedef long GC_signed_word;
#else
  /* Win64 isn't really supported yet, but this is the first step. And	*/
  /* it might cause error messages to show up in more plausible places.	*/
  /* This needs basetsd.h, which is included by windows.h.	 	*/
  typedef ULONG_PTR GC_word;
  typedef LONG_PTR GC_word;
#endif

/* Public read-only variables */

GC_API GC_word GC_gc_no;/* Counter incremented per collection.  	*/
			/* Includes empty GCs at startup.		*/

GC_API int GC_parallel;	/* GC is parallelized for performance on	*/
			/* multiprocessors.  Currently set only		*/
			/* implicitly if collector is built with	*/
			/* -DPARALLEL_MARK and if either:		*/
			/*  Env variable GC_NPROC is set to > 1, or	*/
			/*  GC_NPROC is not set and this is an MP.	*/
			/* If GC_parallel is set, incremental		*/
			/* collection is only partially functional,	*/
			/* and may not be desirable.			*/
			

/* Public R/W variables */

GC_API GC_PTR (*GC_oom_fn) GC_PROTO((size_t bytes_requested));
			/* When there is insufficient memory to satisfy */
			/* an allocation request, we return		*/
			/* (*GC_oom_fn)().  By default this just	*/
			/* returns 0.					*/
			/* If it returns, it must return 0 or a valid	*/
			/* pointer to a previously allocated heap 	*/
			/* object.					*/

GC_API int GC_find_leak;
			/* Do not actually garbage collect, but simply	*/
			/* report inaccessible memory that was not	*/
			/* deallocated with GC_free.  Initial value	*/
			/* is determined by FIND_LEAK macro.		*/

GC_API int GC_all_interior_pointers;
			/* Arrange for pointers to object interiors to	*/
			/* be recognized as valid.  May not be changed	*/
			/* after GC initialization.			*/
			/* Initial value is determined by 		*/
			/* -DALL_INTERIOR_POINTERS.			*/
			/* Unless DONT_ADD_BYTE_AT_END is defined, this	*/
			/* also affects whether sizes are increased by	*/
			/* at least a byte to allow "off the end"	*/
			/* pointer recognition.				*/
			/* MUST BE 0 or 1.				*/

GC_API int GC_quiet;	/* Disable statistics output.  Only matters if	*/
			/* collector has been compiled with statistics	*/
			/* enabled.  This involves a performance cost,	*/
			/* and is thus not the default.			*/

GC_API int GC_finalize_on_demand;
			/* If nonzero, finalizers will only be run in 	*/
			/* response to an explicit GC_invoke_finalizers	*/
			/* call.  The default is determined by whether	*/
			/* the FINALIZE_ON_DEMAND macro is defined	*/
			/* when the collector is built.			*/

GC_API int GC_java_finalization;
			/* Mark objects reachable from finalizable 	*/
			/* objects in a separate postpass.  This makes	*/
			/* it a bit safer to use non-topologically-	*/
			/* ordered finalization.  Default value is	*/
			/* determined by JAVA_FINALIZATION macro.	*/

GC_API void (* GC_finalizer_notifier)();
			/* Invoked by the collector when there are 	*/
			/* objects to be finalized.  Invoked at most	*/
			/* once per GC cycle.  Never invoked unless 	*/
			/* GC_finalize_on_demand is set.		*/
			/* Typically this will notify a finalization	*/
			/* thread, which will call GC_invoke_finalizers */
			/* in response.					*/

GC_API int GC_dont_gc;	/* != 0 ==> Dont collect.  In versions 6.2a1+,	*/
			/* this overrides explicit GC_gcollect() calls.	*/
			/* Used as a counter, so that nested enabling	*/
			/* and disabling work correctly.  Should	*/
			/* normally be updated with GC_enable() and	*/
			/* GC_disable() calls.				*/
			/* Direct assignment to GC_dont_gc is 		*/
			/* deprecated.					*/

GC_API int GC_dont_expand;
			/* Dont expand heap unless explicitly requested */
			/* or forced to.				*/

GC_API int GC_use_entire_heap;
		/* Causes the nonincremental collector to use the	*/
		/* entire heap before collecting.  This was the only 	*/
		/* option for GC versions < 5.0.  This sometimes	*/
		/* results in more large block fragmentation, since	*/
		/* very larg blocks will tend to get broken up		*/
		/* during each GC cycle.  It is likely to result in a	*/
		/* larger working set, but lower collection		*/
		/* frequencies, and hence fewer instructions executed	*/
		/* in the collector.					*/

GC_API int GC_full_freq;    /* Number of partial collections between	*/
			    /* full collections.  Matters only if	*/
			    /* GC_incremental is set.			*/
			    /* Full collections are also triggered if	*/
			    /* the collector detects a substantial	*/
			    /* increase in the number of in-use heap	*/
			    /* blocks.  Values in the tens are now	*/
			    /* perfectly reasonable, unlike for		*/
			    /* earlier GC versions.			*/
			
GC_API GC_word GC_non_gc_bytes;
			/* Bytes not considered candidates for collection. */
			/* Used only to control scheduling of collections. */
			/* Updated by GC_malloc_uncollectable and GC_free. */
			/* Wizards only.				   */

GC_API int GC_no_dls;
			/* Don't register dynamic library data segments. */
			/* Wizards only.  Should be used only if the	 */
			/* application explicitly registers all roots.	 */
			/* In Microsoft Windows environments, this will	 */
			/* usually also prevent registration of the	 */
			/* main data segment as part of the root set.	 */

GC_API GC_word GC_free_space_divisor;
			/* We try to make sure that we allocate at 	*/
			/* least N/GC_free_space_divisor bytes between	*/
			/* collections, where N is the heap size plus	*/
			/* a rough estimate of the root set size.	*/
			/* Initially, GC_free_space_divisor = 4.	*/
			/* Increasing its value will use less space	*/
			/* but more collection time.  Decreasing it	*/
			/* will appreciably decrease collection time	*/
			/* at the expense of space.			*/
			/* GC_free_space_divisor = 1 will effectively	*/
			/* disable collections.				*/

GC_API GC_word GC_max_retries;
			/* The maximum number of GCs attempted before	*/
			/* reporting out of memory after heap		*/
			/* expansion fails.  Initially 0.		*/
			

GC_API char *GC_stackbottom;	/* Cool end of user stack.		*/
				/* May be set in the client prior to	*/
				/* calling any GC_ routines.  This	*/
				/* avoids some overhead, and 		*/
				/* potentially some signals that can 	*/
				/* confuse debuggers.  Otherwise the	*/
				/* collector attempts to set it 	*/
				/* automatically.			*/
				/* For multithreaded code, this is the	*/
				/* cold end of the stack for the	*/
				/* primordial thread.			*/	
				
GC_API int GC_dont_precollect;  /* Don't collect as part of 		*/
				/* initialization.  Should be set only	*/
				/* if the client wants a chance to	*/
				/* manually initialize the root set	*/
				/* before the first collection.		*/
				/* Interferes with blacklisting.	*/
				/* Wizards only.			*/

GC_API unsigned long GC_time_limit;
				/* If incremental collection is enabled, */
				/* We try to terminate collections	 */
				/* after this many milliseconds.  Not a	 */
				/* hard time bound.  Setting this to 	 */
				/* GC_TIME_UNLIMITED will essentially	 */
				/* disable incremental collection while  */
				/* leaving generational collection	 */
				/* enabled.	 			 */
#	define GC_TIME_UNLIMITED 999999
				/* Setting GC_time_limit to this value	 */
				/* will disable the "pause time exceeded"*/
				/* tests.				 */

/* Public procedures */

/* Initialize the collector.  This is only required when using thread-local
 * allocation, since unlike the regular allocation routines, GC_local_malloc
 * is not self-initializing.  If you use GC_local_malloc you should arrange
 * to call this somehow (e.g. from a constructor) before doing any allocation.
 * For win32 threads, it needs to be called explicitly.
 */
GC_API void GC_init GC_PROTO((void));

/*
 * general purpose allocation routines, with roughly malloc calling conv.
 * The atomic versions promise that no relevant pointers are contained
 * in the object.  The nonatomic versions guarantee that the new object
 * is cleared.  GC_malloc_stubborn promises that no changes to the object
 * will occur after GC_end_stubborn_change has been called on the
 * result of GC_malloc_stubborn. GC_malloc_uncollectable allocates an object
 * that is scanned for pointers to collectable objects, but is not itself
 * collectable.  The object is scanned even if it does not appear to
 * be reachable.  GC_malloc_uncollectable and GC_free called on the resulting
 * object implicitly update GC_non_gc_bytes appropriately.
 *
 * Note that the GC_malloc_stubborn support is stubbed out by default
 * starting in 6.0.  GC_malloc_stubborn is an alias for GC_malloc unless
 * the collector is built with STUBBORN_ALLOC defined.
 */
GC_API GC_PTR GC_malloc GC_PROTO((size_t size_in_bytes));
GC_API GC_PTR GC_malloc_atomic GC_PROTO((size_t size_in_bytes));
GC_API GC_PTR GC_malloc_uncollectable GC_PROTO((size_t size_in_bytes));
GC_API GC_PTR GC_malloc_stubborn GC_PROTO((size_t size_in_bytes));

/* The following is only defined if the library has been suitably	*/
/* compiled:								*/
GC_API GC_PTR GC_malloc_atomic_uncollectable GC_PROTO((size_t size_in_bytes));

#ifdef WITH_PREALLOC_OPT
  /* Added for the preallocation optimization (by Alexandru Salcianu):
     allocates a heap region of "size_in_bytes" bytes that is treated as
     a normal area by the "mark" phase of the garbage collector, but
     it's ignored by the "sweep" phase (i.e., it's never collected).
     Objects reachable only from an unreachable "prealloc" object can be
     collected.  */
  GC_API GC_PTR GC_malloc_prealloc GC_PROTO((size_t size_in_bytes));
#endif

/* Explicitly deallocate an object.  Dangerous if used incorrectly.     */
/* Requires a pointer to the base of an object.				*/
/* If the argument is stubborn, it should not be changeable when freed. */
/* An object should not be enable for finalization when it is 		*/
/* explicitly deallocated.						*/
/* GC_free(0) is a no-op, as required by ANSI C for free.		*/
GC_API void GC_free GC_PROTO((GC_PTR object_addr));

/*
 * Stubborn objects may be changed only if the collector is explicitly informed.
 * The collector is implicitly informed of coming change when such
 * an object is first allocated.  The following routines inform the
 * collector that an object will no longer be changed, or that it will
 * once again be changed.  Only nonNIL pointer stores into the object
 * are considered to be changes.  The argument to GC_end_stubborn_change
 * must be exacly the value returned by GC_malloc_stubborn or passed to
 * GC_change_stubborn.  (In the second case it may be an interior pointer
 * within 512 bytes of the beginning of the objects.)
 * There is a performance penalty for allowing more than
 * one stubborn object to be changed at once, but it is acceptable to
 * do so.  The same applies to dropping stubborn objects that are still
 * changeable.
 */
GC_API void GC_change_stubborn GC_PROTO((GC_PTR));
GC_API void GC_end_stubborn_change GC_PROTO((GC_PTR));

/* Return a pointer to the base (lowest address) of an object given	*/
/* a pointer to a location within the object.				*/
/* I.e. map an interior pointer to the corresponding bas pointer.	*/
/* Note that with debugging allocation, this returns a pointer to the	*/
/* actual base of the object, i.e. the debug information, not to	*/
/* the base of the user object.						*/
/* Return 0 if displaced_pointer doesn't point to within a valid	*/
/* object.								*/
GC_API GC_PTR GC_base GC_PROTO((GC_PTR displaced_pointer));

/* Given a pointer to the base of an object, return its size in bytes.	*/
/* The returned size may be slightly larger than what was originally	*/
/* requested.								*/
GC_API size_t GC_size GC_PROTO((GC_PTR object_addr));

/* For compatibility with C library.  This is occasionally faster than	*/
/* a malloc followed by a bcopy.  But if you rely on that, either here	*/
/* or with the standard C library, your code is broken.  In my		*/
/* opinion, it shouldn't have been invented, but now we're stuck. -HB	*/
/* The resulting object has the same kind as the original.		*/
/* If the argument is stubborn, the result will have changes enabled.	*/
/* It is an error to have changes enabled for the original object.	*/
/* Follows ANSI comventions for NULL old_object.			*/
GC_API GC_PTR GC_realloc
	GC_PROTO((GC_PTR old_object, size_t new_size_in_bytes));
				   
/* Explicitly increase the heap size.	*/
/* Returns 0 on failure, 1 on success.  */
GC_API int GC_expand_hp GC_PROTO((size_t number_of_bytes));

/* Limit the heap size to n bytes.  Useful when you're debugging, 	*/
/* especially on systems that don't handle running out of memory well.	*/
/* n == 0 ==> unbounded.  This is the default.				*/
GC_API void GC_set_max_heap_size GC_PROTO((GC_word n));

/* Inform the collector that a certain section of statically allocated	*/
/* memory contains no pointers to garbage collected memory.  Thus it 	*/
/* need not be scanned.  This is sometimes important if the application */
/* maps large read/write files into the address space, which could be	*/
/* mistaken for dynamic library data segments on some systems.		*/
GC_API void GC_exclude_static_roots GC_PROTO((GC_PTR start, GC_PTR finish));

/* Clear the set of root segments.  Wizards only. */
GC_API void GC_clear_roots GC_PROTO((void));

/* Add a root segment.  Wizards only. */
GC_API void GC_add_roots GC_PROTO((char * low_address,
				   char * high_address_plus_1));

/* Remove a root segment.  Wizards only. */
GC_API void GC_remove_roots GC_PROTO((char * low_address, 
    char * high_address_plus_1));

/* Add a displacement to the set of those considered valid by the	*/
/* collector.  GC_register_displacement(n) means that if p was returned */
/* by GC_malloc, then (char *)p + n will be considered to be a valid	*/
/* pointer to p.  N must be small and less than the size of p.		*/
/* (All pointers to the interior of objects from the stack are		*/
/* considered valid in any case.  This applies to heap objects and	*/
/* static data.)							*/
/* Preferably, this should be called before any other GC procedures.	*/
/* Calling it later adds to the probability of excess memory		*/
/* retention.								*/
/* This is a no-op if the collector has recognition of			*/
/* arbitrary interior pointers enabled, which is now the default.	*/
GC_API void GC_register_displacement GC_PROTO((GC_word n));

/* The following version should be used if any debugging allocation is	*/
/* being done.								*/
GC_API void GC_debug_register_displacement GC_PROTO((GC_word n));

/* Explicitly trigger a full, world-stop collection. 	*/
GC_API void GC_gcollect GC_PROTO((void));

/* Trigger a full world-stopped collection.  Abort the collection if 	*/
/* and when stop_func returns a nonzero value.  Stop_func will be 	*/
/* called frequently, and should be reasonably fast.  This works even	*/
/* if virtual dirty bits, and hence incremental collection is not 	*/
/* available for this architecture.  Collections can be aborted faster	*/
/* than normal pause times for incremental collection.  However,	*/
/* aborted collections do no useful work; the next collection needs	*/
/* to start from the beginning.						*/
/* Return 0 if the collection was aborted, 1 if it succeeded.		*/
typedef int (* GC_stop_func) GC_PROTO((void));
GC_API int GC_try_to_collect GC_PROTO((GC_stop_func stop_func));

/* Return the number of bytes in the heap.  Excludes collector private	*/
/* data structures.  Includes empty blocks and fragmentation loss.	*/
/* Includes some pages that were allocated but never written.		*/
GC_API size_t GC_get_heap_size GC_PROTO((void));

/* Return a lower bound on the number of free bytes in the heap.	*/
GC_API size_t GC_get_free_bytes GC_PROTO((void));

/* Return the number of bytes allocated since the last collection.	*/
GC_API size_t GC_get_bytes_since_gc GC_PROTO((void));

/* Return the total number of bytes allocated in this process.		*/
/* Never decreases, except due to wrapping.				*/
GC_API size_t GC_get_total_bytes GC_PROTO((void));

/* Disable garbage collection.  Even GC_gcollect calls will be 		*/
/* ineffective.								*/
GC_API void GC_disable GC_PROTO((void));

/* Reenable garbage collection.  GC_disable() and GC_enable() calls 	*/
/* nest.  Garbage collection is enabled if the number of calls to both	*/
/* both functions is equal.						*/
GC_API void GC_enable GC_PROTO((void));

/* Enable incremental/generational collection.	*/
/* Not advisable unless dirty bits are 		*/
/* available or most heap objects are		*/
/* pointerfree(atomic) or immutable.		*/
/* Don't use in leak finding mode.		*/
/* Ignored if GC_dont_gc is true.		*/
/* Only the generational piece of this is	*/
/* functional if GC_parallel is TRUE		*/
/* or if GC_time_limit is GC_TIME_UNLIMITED.	*/
/* Causes GC_local_gcj_malloc() to revert to	*/
/* locked allocation.  Must be called 		*/
/* before any GC_local_gcj_malloc() calls.	*/
GC_API void GC_enable_incremental GC_PROTO((void));

/* Does incremental mode write-protect pages?  Returns zero or	*/
/* more of the following, or'ed together:			*/
#define GC_PROTECTS_POINTER_HEAP  1 /* May protect non-atomic objs.	*/
#define GC_PROTECTS_PTRFREE_HEAP  2
#define GC_PROTECTS_STATIC_DATA   4 /* Curently never.			*/
#define GC_PROTECTS_STACK	  8 /* Probably impractical.		*/

#define GC_PROTECTS_NONE 0
GC_API int GC_incremental_protection_needs GC_PROTO((void));

/* Perform some garbage collection work, if appropriate.	*/
/* Return 0 if there is no more work to be done.		*/
/* Typically performs an amount of work corresponding roughly	*/
/* to marking from one page.  May do more work if further	*/
/* progress requires it, e.g. if incremental collection is	*/
/* disabled.  It is reasonable to call this in a wait loop	*/
/* until it returns 0.						*/
GC_API int GC_collect_a_little GC_PROTO((void));

/* Allocate an object of size lb bytes.  The client guarantees that	*/
/* as long as the object is live, it will be referenced by a pointer	*/
/* that points to somewhere within the first 256 bytes of the object.	*/
/* (This should normally be declared volatile to prevent the compiler	*/
/* from invalidating this assertion.)  This routine is only useful	*/
/* if a large array is being allocated.  It reduces the chance of 	*/
/* accidentally retaining such an array as a result of scanning an	*/
/* integer that happens to be an address inside the array.  (Actually,	*/
/* it reduces the chance of the allocator not finding space for such	*/
/* an array, since it will try hard to avoid introducing such a false	*/
/* reference.)  On a SunOS 4.X or MS Windows system this is recommended */
/* for arrays likely to be larger than 100K or so.  For other systems,	*/
/* or if the collector is not configured to recognize all interior	*/
/* pointers, the threshold is normally much higher.			*/
GC_API GC_PTR GC_malloc_ignore_off_page GC_PROTO((size_t lb));
GC_API GC_PTR GC_malloc_atomic_ignore_off_page GC_PROTO((size_t lb));

#if defined(__sgi) && !defined(__GNUC__) && _COMPILER_VERSION >= 720
#   define GC_ADD_CALLER
#   define GC_RETURN_ADDR (GC_word)__return_address
#endif

#ifdef __linux__
# include <features.h>
# if (__GLIBC__ == 2 && __GLIBC_MINOR__ >= 1 || __GLIBC__ > 2) \
     && !defined(__ia64__)
#   ifndef GC_HAVE_BUILTIN_BACKTRACE
#     define GC_HAVE_BUILTIN_BACKTRACE
#   endif
# endif
# if defined(__i386__) || defined(__x86_64__)
#   define GC_CAN_SAVE_CALL_STACKS
# endif
#endif

#if defined(GC_HAVE_BUILTIN_BACKTRACE) && !defined(GC_CAN_SAVE_CALL_STACKS)
# define GC_CAN_SAVE_CALL_STACKS
#endif

#if defined(__sparc__)
#   define GC_CAN_SAVE_CALL_STACKS
#endif

/* If we're on an a platform on which we can't save call stacks, but	*/
/* gcc is normally used, we go ahead and define GC_ADD_CALLER.  	*/
/* We make this decision independent of whether gcc is actually being	*/
/* used, in order to keep the interface consistent, and allow mixing	*/
/* of compilers.							*/
/* This may also be desirable if it is possible but expensive to	*/
/* retrieve the call chain.						*/
#if (defined(__linux__) || defined(__NetBSD__) || defined(__OpenBSD__) \
     || defined(__FreeBSD__)) & !defined(GC_CAN_SAVE_CALL_STACKS)
# define GC_ADD_CALLER
# if __GNUC__ >= 3 || (__GNUC__ == 2 && __GNUC_MINOR__ >= 95) 
    /* gcc knows how to retrieve return address, but we don't know */
    /* how to generate call stacks.				   */
#   define GC_RETURN_ADDR (GC_word)__builtin_return_address(0)
# else
    /* Just pass 0 for gcc compatibility. */
#   define GC_RETURN_ADDR 0
# endif
#endif

#ifdef GC_ADD_CALLER
#  define GC_EXTRAS GC_RETURN_ADDR, __FILE__, __LINE__
#  define GC_EXTRA_PARAMS GC_word ra, GC_CONST char * s, int i
#else
#  define GC_EXTRAS __FILE__, __LINE__
#  define GC_EXTRA_PARAMS GC_CONST char * s, int i
#endif

/* Debugging (annotated) allocation.  GC_gcollect will check 		*/
/* objects allocated in this way for overwrites, etc.			*/
GC_API GC_PTR GC_debug_malloc
	GC_PROTO((size_t size_in_bytes, GC_EXTRA_PARAMS));
GC_API GC_PTR GC_debug_malloc_atomic
	GC_PROTO((size_t size_in_bytes, GC_EXTRA_PARAMS));
GC_API GC_PTR GC_debug_malloc_uncollectable
	GC_PROTO((size_t size_in_bytes, GC_EXTRA_PARAMS));
GC_API GC_PTR GC_debug_malloc_stubborn
	GC_PROTO((size_t size_in_bytes, GC_EXTRA_PARAMS));
GC_API GC_PTR GC_debug_malloc_ignore_off_page
	GC_PROTO((size_t size_in_bytes, GC_EXTRA_PARAMS));
GC_API GC_PTR GC_debug_malloc_atomic_ignore_off_page
	GC_PROTO((size_t size_in_bytes, GC_EXTRA_PARAMS));
GC_API void GC_debug_free GC_PROTO((GC_PTR object_addr));
GC_API GC_PTR GC_debug_realloc
	GC_PROTO((GC_PTR old_object, size_t new_size_in_bytes,
  		  GC_EXTRA_PARAMS));
GC_API void GC_debug_change_stubborn GC_PROTO((GC_PTR));
GC_API void GC_debug_end_stubborn_change GC_PROTO((GC_PTR));

/* Routines that allocate objects with debug information (like the 	*/
/* above), but just fill in dummy file and line number information.	*/
/* Thus they can serve as drop-in malloc/realloc replacements.  This	*/
/* can be useful for two reasons:  					*/
/* 1) It allows the collector to be built with DBG_HDRS_ALL defined	*/
/*    even if some allocation calls come from 3rd party libraries	*/
/*    that can't be recompiled.						*/
/* 2) On some platforms, the file and line information is redundant,	*/
/*    since it can be reconstructed from a stack trace.  On such	*/
/*    platforms it may be more convenient not to recompile, e.g. for	*/
/*    leak detection.  This can be accomplished by instructing the	*/
/*    linker to replace malloc/realloc with these.			*/
GC_API GC_PTR GC_debug_malloc_replacement GC_PROTO((size_t size_in_bytes));
GC_API GC_PTR GC_debug_realloc_replacement
	      GC_PROTO((GC_PTR object_addr, size_t size_in_bytes));
  			 	 
# ifdef GC_DEBUG
#   define GC_MALLOC(sz) GC_debug_malloc(sz, GC_EXTRAS)
#   define GC_MALLOC_ATOMIC(sz) GC_debug_malloc_atomic(sz, GC_EXTRAS)
#   define GC_MALLOC_UNCOLLECTABLE(sz) \
			GC_debug_malloc_uncollectable(sz, GC_EXTRAS)
#   define GC_MALLOC_IGNORE_OFF_PAGE(sz) \
			GC_debug_malloc_ignore_off_page(sz, GC_EXTRAS)
#   define GC_MALLOC_ATOMIC_IGNORE_OFF_PAGE(sz) \
			GC_debug_malloc_atomic_ignore_off_page(sz, GC_EXTRAS)
#   define GC_REALLOC(old, sz) GC_debug_realloc(old, sz, GC_EXTRAS)
#   define GC_FREE(p) GC_debug_free(p)
#   define GC_REGISTER_FINALIZER(p, f, d, of, od) \
	GC_debug_register_finalizer(p, f, d, of, od)
#   define GC_REGISTER_FINALIZER_IGNORE_SELF(p, f, d, of, od) \
	GC_debug_register_finalizer_ignore_self(p, f, d, of, od)
#   define GC_REGISTER_FINALIZER_NO_ORDER(p, f, d, of, od) \
	GC_debug_register_finalizer_no_order(p, f, d, of, od)
#   define GC_MALLOC_STUBBORN(sz) GC_debug_malloc_stubborn(sz, GC_EXTRAS);
#   define GC_CHANGE_STUBBORN(p) GC_debug_change_stubborn(p)
#   define GC_END_STUBBORN_CHANGE(p) GC_debug_end_stubborn_change(p)
#   define GC_GENERAL_REGISTER_DISAPPEARING_LINK(link, obj) \
	GC_general_register_disappearing_link(link, GC_base(obj))
#   define GC_REGISTER_DISPLACEMENT(n) GC_debug_register_displacement(n)
# else
#   define GC_MALLOC(sz) GC_malloc(sz)
#   define GC_MALLOC_ATOMIC(sz) GC_malloc_atomic(sz)
#   define GC_MALLOC_UNCOLLECTABLE(sz) GC_malloc_uncollectable(sz)
#   define GC_MALLOC_IGNORE_OFF_PAGE(sz) \
			GC_malloc_ignore_off_page(sz)
#   define GC_MALLOC_ATOMIC_IGNORE_OFF_PAGE(sz) \
			GC_malloc_atomic_ignore_off_page(sz)
#   define GC_REALLOC(old, sz) GC_realloc(old, sz)
#   define GC_FREE(p) GC_free(p)
#   define GC_REGISTER_FINALIZER(p, f, d, of, od) \
	GC_register_finalizer(p, f, d, of, od)
#   define GC_REGISTER_FINALIZER_IGNORE_SELF(p, f, d, of, od) \
	GC_register_finalizer_ignore_self(p, f, d, of, od)
#   define GC_REGISTER_FINALIZER_NO_ORDER(p, f, d, of, od) \
	GC_register_finalizer_no_order(p, f, d, of, od)
#   define GC_MALLOC_STUBBORN(sz) GC_malloc_stubborn(sz)
#   define GC_CHANGE_STUBBORN(p) GC_change_stubborn(p)
#   define GC_END_STUBBORN_CHANGE(p) GC_end_stubborn_change(p)
#   define GC_GENERAL_REGISTER_DISAPPEARING_LINK(link, obj) \
	GC_general_register_disappearing_link(link, obj)
#   define GC_REGISTER_DISPLACEMENT(n) GC_register_displacement(n)
# endif
/* The following are included because they are often convenient, and	*/
/* reduce the chance for a misspecifed size argument.  But calls may	*/
/* expand to something syntactically incorrect if t is a complicated	*/
/* type expression.  							*/
# define GC_NEW(t) (t *)GC_MALLOC(sizeof (t))
# define GC_NEW_ATOMIC(t) (t *)GC_MALLOC_ATOMIC(sizeof (t))
# define GC_NEW_STUBBORN(t) (t *)GC_MALLOC_STUBBORN(sizeof (t))
# define GC_NEW_UNCOLLECTABLE(t) (t *)GC_MALLOC_UNCOLLECTABLE(sizeof (t))

/* Finalization.  Some of these primitives are grossly unsafe.		*/
/* The idea is to make them both cheap, and sufficient to build		*/
/* a safer layer, closer to Modula-3, Java, or PCedar finalization.	*/
/* The interface represents my conclusions from a long discussion	*/
/* with Alan Demers, Dan Greene, Carl Hauser, Barry Hayes, 		*/
/* Christian Jacobi, and Russ Atkinson.  It's not perfect, and		*/
/* probably nobody else agrees with it.	    Hans-J. Boehm  3/13/92	*/
typedef void (*GC_finalization_proc)
  	GC_PROTO((GC_PTR obj, GC_PTR client_data));

GC_API void GC_register_finalizer
    	GC_PROTO((GC_PTR obj, GC_finalization_proc fn, GC_PTR cd,
		  GC_finalization_proc *ofn, GC_PTR *ocd));
GC_API void GC_debug_register_finalizer
    	GC_PROTO((GC_PTR obj, GC_finalization_proc fn, GC_PTR cd,
		  GC_finalization_proc *ofn, GC_PTR *ocd));
	/* When obj is no longer accessible, invoke		*/
	/* (*fn)(obj, cd).  If a and b are inaccessible, and	*/
	/* a points to b (after disappearing links have been	*/
	/* made to disappear), then only a will be		*/
	/* finalized.  (If this does not create any new		*/
	/* pointers to b, then b will be finalized after the	*/
	/* next collection.)  Any finalizable object that	*/
	/* is reachable from itself by following one or more	*/
	/* pointers will not be finalized (or collected).	*/
	/* Thus cycles involving finalizable objects should	*/
	/* be avoided, or broken by disappearing links.		*/
	/* All but the last finalizer registered for an object  */
	/* is ignored.						*/
	/* Finalization may be removed by passing 0 as fn.	*/
	/* Finalizers are implicitly unregistered just before   */
	/* they are invoked.					*/
	/* The old finalizer and client data are stored in	*/
	/* *ofn and *ocd.					*/ 
	/* Fn is never invoked on an accessible object,		*/
	/* provided hidden pointers are converted to real 	*/
	/* pointers only if the allocation lock is held, and	*/
	/* such conversions are not performed by finalization	*/
	/* routines.						*/
	/* If GC_register_finalizer is aborted as a result of	*/
	/* a signal, the object may be left with no		*/
	/* finalization, even if neither the old nor new	*/
	/* finalizer were NULL.					*/
	/* Obj should be the nonNULL starting address of an 	*/
	/* object allocated by GC_malloc or friends.		*/
	/* Note that any garbage collectable object referenced	*/
	/* by cd will be considered accessible until the	*/
	/* finalizer is invoked.				*/

/* Another versions of the above follow.  It ignores		*/
/* self-cycles, i.e. pointers from a finalizable object to	*/
/* itself.  There is a stylistic argument that this is wrong,	*/
/* but it's unavoidable for C++, since the compiler may		*/
/* silently introduce these.  It's also benign in that specific	*/
/* case.  And it helps if finalizable objects are split to	*/
/* avoid cycles.						*/
/* Note that cd will still be viewed as accessible, even if it	*/
/* refers to the object itself.					*/
GC_API void GC_register_finalizer_ignore_self
	GC_PROTO((GC_PTR obj, GC_finalization_proc fn, GC_PTR cd,
		  GC_finalization_proc *ofn, GC_PTR *ocd));
GC_API void GC_debug_register_finalizer_ignore_self
	GC_PROTO((GC_PTR obj, GC_finalization_proc fn, GC_PTR cd,
		  GC_finalization_proc *ofn, GC_PTR *ocd));

/* Another version of the above.  It ignores all cycles.        */
/* It should probably only be used by Java implementations.     */
/* Note that cd will still be viewed as accessible, even if it	*/
/* refers to the object itself.					*/
GC_API void GC_register_finalizer_no_order
	GC_PROTO((GC_PTR obj, GC_finalization_proc fn, GC_PTR cd,
		  GC_finalization_proc *ofn, GC_PTR *ocd));
GC_API void GC_debug_register_finalizer_no_order
	GC_PROTO((GC_PTR obj, GC_finalization_proc fn, GC_PTR cd,
		  GC_finalization_proc *ofn, GC_PTR *ocd));


/* The following routine may be used to break cycles between	*/
/* finalizable objects, thus causing cyclic finalizable		*/
/* objects to be finalized in the correct order.  Standard	*/
/* use involves calling GC_register_disappearing_link(&p),	*/
/* where p is a pointer that is not followed by finalization	*/
/* code, and should not be considered in determining 		*/
/* finalization order.						*/
GC_API int GC_register_disappearing_link GC_PROTO((GC_PTR * /* link */));
	/* Link should point to a field of a heap allocated 	*/
	/* object obj.  *link will be cleared when obj is	*/
	/* found to be inaccessible.  This happens BEFORE any	*/
	/* finalization code is invoked, and BEFORE any		*/
	/* decisions about finalization order are made.		*/
	/* This is useful in telling the finalizer that 	*/
	/* some pointers are not essential for proper		*/
	/* finalization.  This may avoid finalization cycles.	*/
	/* Note that obj may be resurrected by another		*/
	/* finalizer, and thus the clearing of *link may	*/
	/* be visible to non-finalization code.  		*/
	/* There's an argument that an arbitrary action should  */
	/* be allowed here, instead of just clearing a pointer. */
	/* But this causes problems if that action alters, or 	*/
	/* examines connectivity.				*/
	/* Returns 1 if link was already registered, 0		*/
	/* otherwise.						*/
	/* Only exists for backward compatibility.  See below:	*/
	
GC_API int GC_general_register_disappearing_link
	GC_PROTO((GC_PTR * /* link */, GC_PTR obj));
	/* A slight generalization of the above. *link is	*/
	/* cleared when obj first becomes inaccessible.  This	*/
	/* can be used to implement weak pointers easily and	*/
	/* safely. Typically link will point to a location	*/
	/* holding a disguised pointer to obj.  (A pointer 	*/
	/* inside an "atomic" object is effectively  		*/
	/* disguised.)   In this way soft			*/
	/* pointers are broken before any object		*/
	/* reachable from them are finalized.  Each link	*/
	/* May be registered only once, i.e. with one obj	*/
	/* value.  This was added after a long email discussion */
	/* with John Ellis.					*/
	/* Obj must be a pointer to the first word of an object */
	/* we allocated.  It is unsafe to explicitly deallocate */
	/* the object containing link.  Explicitly deallocating */
	/* obj may or may not cause link to eventually be	*/
	/* cleared.						*/
GC_API int GC_unregister_disappearing_link GC_PROTO((GC_PTR * /* link */));
	/* Returns 0 if link was not actually registered.	*/
	/* Undoes a registration by either of the above two	*/
	/* routines.						*/

/* Returns !=0  if GC_invoke_finalizers has something to do. 		*/
GC_API int GC_should_invoke_finalizers GC_PROTO((void));

GC_API int GC_invoke_finalizers GC_PROTO((void));
	/* Run finalizers for all objects that are ready to	*/
	/* be finalized.  Return the number of finalizers	*/
	/* that were run.  Normally this is also called		*/
	/* implicitly during some allocations.	If		*/
	/* GC-finalize_on_demand is nonzero, it must be called	*/
	/* explicitly.						*/

/* GC_set_warn_proc can be used to redirect or filter warning messages.	*/
/* p may not be a NULL pointer.						*/
typedef void (*GC_warn_proc) GC_PROTO((char *msg, GC_word arg));
GC_API GC_warn_proc GC_set_warn_proc GC_PROTO((GC_warn_proc p));
    /* Returns old warning procedure.	*/

GC_API GC_word GC_set_free_space_divisor GC_PROTO((GC_word value));
    /* Set free_space_divisor.  See above for definition.	*/
    /* Returns old value.					*/
	
/* The following is intended to be used by a higher level	*/
/* (e.g. Java-like) finalization facility.  It is expected	*/
/* that finalization code will arrange for hidden pointers to	*/
/* disappear.  Otherwise objects can be accessed after they	*/
/* have been collected.						*/
/* Note that putting pointers in atomic objects or in 		*/
/* nonpointer slots of "typed" objects is equivalent to 	*/
/* disguising them in this way, and may have other advantages.	*/
# if defined(I_HIDE_POINTERS) || defined(GC_I_HIDE_POINTERS)
    typedef GC_word GC_hidden_pointer;
#   define HIDE_POINTER(p) (~(GC_hidden_pointer)(p))
#   define REVEAL_POINTER(p) ((GC_PTR)(HIDE_POINTER(p)))
    /* Converting a hidden pointer to a real pointer requires verifying	*/
    /* that the object still exists.  This involves acquiring the  	*/
    /* allocator lock to avoid a race with the collector.		*/
# endif /* I_HIDE_POINTERS */

typedef GC_PTR (*GC_fn_type) GC_PROTO((GC_PTR client_data));
GC_API GC_PTR GC_call_with_alloc_lock
        	GC_PROTO((GC_fn_type fn, GC_PTR client_data));

/* The following routines are primarily intended for use with a 	*/
/* preprocessor which inserts calls to check C pointer arithmetic.	*/
/* They indicate failure by invoking the corresponding _print_proc.	*/

/* Check that p and q point to the same object.  		*/
/* Fail conspicuously if they don't.				*/
/* Returns the first argument.  				*/
/* Succeeds if neither p nor q points to the heap.		*/
/* May succeed if both p and q point to between heap objects.	*/
GC_API GC_PTR GC_same_obj GC_PROTO((GC_PTR p, GC_PTR q));

/* Checked pointer pre- and post- increment operations.  Note that	*/
/* the second argument is in units of bytes, not multiples of the	*/
/* object size.  This should either be invoked from a macro, or the	*/
/* call should be automatically generated.				*/
GC_API GC_PTR GC_pre_incr GC_PROTO((GC_PTR *p, size_t how_much));
GC_API GC_PTR GC_post_incr GC_PROTO((GC_PTR *p, size_t how_much));

/* Check that p is visible						*/
/* to the collector as a possibly pointer containing location.		*/
/* If it isn't fail conspicuously.					*/
/* Returns the argument in all cases.  May erroneously succeed		*/
/* in hard cases.  (This is intended for debugging use with		*/
/* untyped allocations.  The idea is that it should be possible, though	*/
/* slow, to add such a call to all indirect pointer stores.)		*/
/* Currently useless for multithreaded worlds.				*/
GC_API GC_PTR GC_is_visible GC_PROTO((GC_PTR p));

/* Check that if p is a pointer to a heap page, then it points to	*/
/* a valid displacement within a heap object.				*/
/* Fail conspicuously if this property does not hold.			*/
/* Uninteresting with GC_all_interior_pointers.				*/
/* Always returns its argument.						*/
GC_API GC_PTR GC_is_valid_displacement GC_PROTO((GC_PTR	p));

/* Safer, but slow, pointer addition.  Probably useful mainly with 	*/
/* a preprocessor.  Useful only for heap pointers.			*/
#ifdef GC_DEBUG
#   define GC_PTR_ADD3(x, n, type_of_result) \
	((type_of_result)GC_same_obj((x)+(n), (x)))
#   define GC_PRE_INCR3(x, n, type_of_result) \
	((type_of_result)GC_pre_incr(&(x), (n)*sizeof(*x))
#   define GC_POST_INCR2(x, type_of_result) \
	((type_of_result)GC_post_incr(&(x), sizeof(*x))
#   ifdef __GNUC__
#       define GC_PTR_ADD(x, n) \
	    GC_PTR_ADD3(x, n, typeof(x))
#       define GC_PRE_INCR(x, n) \
	    GC_PRE_INCR3(x, n, typeof(x))
#       define GC_POST_INCR(x, n) \
	    GC_POST_INCR3(x, typeof(x))
#   else
	/* We can't do this right without typeof, which ANSI	*/
	/* decided was not sufficiently useful.  Repeatedly	*/
	/* mentioning the arguments seems too dangerous to be	*/
	/* useful.  So does not casting the result.		*/
#   	define GC_PTR_ADD(x, n) ((x)+(n))
#   endif
#else	/* !GC_DEBUG */
#   define GC_PTR_ADD3(x, n, type_of_result) ((x)+(n))
#   define GC_PTR_ADD(x, n) ((x)+(n))
#   define GC_PRE_INCR3(x, n, type_of_result) ((x) += (n))
#   define GC_PRE_INCR(x, n) ((x) += (n))
#   define GC_POST_INCR2(x, n, type_of_result) ((x)++)
#   define GC_POST_INCR(x, n) ((x)++)
#endif

/* Safer assignment of a pointer to a nonstack location.	*/
#ifdef GC_DEBUG
# ifdef __STDC__
#   define GC_PTR_STORE(p, q) \
	(*(void **)GC_is_visible(p) = GC_is_valid_displacement(q))
# else
#   define GC_PTR_STORE(p, q) \
	(*(char **)GC_is_visible(p) = GC_is_valid_displacement(q))
# endif
#else /* !GC_DEBUG */
#   define GC_PTR_STORE(p, q) *((p) = (q))
#endif

/* Functions called to report pointer checking errors */
GC_API void (*GC_same_obj_print_proc) GC_PROTO((GC_PTR p, GC_PTR q));

GC_API void (*GC_is_valid_displacement_print_proc)
	GC_PROTO((GC_PTR p));

GC_API void (*GC_is_visible_print_proc)
	GC_PROTO((GC_PTR p));

#if defined(GC_USER_THREADS)
#include <signal.h>
#define dlopen GC_dlopen
#endif

/* For pthread support, we generally need to intercept a number of 	*/
/* thread library calls.  We do that here by macro defining them.	*/

#if !defined(GC_USE_LD_WRAP) && \
    (defined(GC_PTHREADS) || defined(GC_SOLARIS_THREADS))
# include "gc_pthread_redirects.h"
#endif

# if defined(PCR) || defined(GC_SOLARIS_THREADS) || \
     defined(GC_PTHREADS) || defined(GC_WIN32_THREADS)
   	/* Any flavor of threads except SRC_M3.	*/
/* This returns a list of objects, linked through their first		*/
/* word.  Its use can greatly reduce lock contention problems, since	*/
/* the allocation lock can be acquired and released many fewer times.	*/
/* lb must be large enough to hold the pointer field.			*/
/* It is used internally by gc_local_alloc.h, which provides a simpler	*/
/* programming interface on Linux.					*/
GC_PTR GC_malloc_many(size_t lb);
#define GC_NEXT(p) (*(GC_PTR *)(p)) 	/* Retrieve the next element	*/
					/* in returned list.		*/
extern void GC_thr_init();	/* Needed for Solaris/X86	*/

#endif /* THREADS && !SRC_M3 */

#if defined(GC_WIN32_THREADS) && !defined(__CYGWIN32__) && !defined(__CYGWIN__)
# include <windows.h>

  /*
   * All threads must be created using GC_CreateThread, so that they will be
   * recorded in the thread table.  For backwards compatibility, this is not
   * technically true if the GC is built as a dynamic library, since it can
   * and does then use DllMain to keep track of thread creations.  But new code
   * should be built to call GC_CreateThread.
   */
   GC_API HANDLE WINAPI GC_CreateThread(
      LPSECURITY_ATTRIBUTES lpThreadAttributes,
      DWORD dwStackSize, LPTHREAD_START_ROUTINE lpStartAddress,
      LPVOID lpParameter, DWORD dwCreationFlags, LPDWORD lpThreadId );

# if defined(_WIN32_WCE)
  /*
   * win32_threads.c implements the real WinMain, which will start a new thread
   * to call GC_WinMain after initializing the garbage collector.
   */
  int WINAPI GC_WinMain(
      HINSTANCE hInstance,
      HINSTANCE hPrevInstance,
      LPWSTR lpCmdLine,
      int nCmdShow );

#  ifndef GC_BUILD
#    define WinMain GC_WinMain
#    define CreateThread GC_CreateThread
#  endif
# endif /* defined(_WIN32_WCE) */

#endif /* defined(GC_WIN32_THREADS)  && !cygwin */

 /*
  * Fully portable code should call GC_INIT() from the main program
  * before making any other GC_ calls.  On most platforms this is a
  * no-op and the collector self-initializes.  But a number of platforms
  * make that too hard.
  */
#if (defined(sparc) || defined(__sparc)) && defined(sun)
    /*
     * If you are planning on putting
     * the collector in a SunOS 5 dynamic library, you need to call GC_INIT()
     * from the statically loaded program section.
     * This circumvents a Solaris 2.X (X<=4) linker bug.
     */
#   define GC_INIT() { extern end, etext; \
		       GC_noop(&end, &etext); }
#else
# if defined(__CYGWIN32__) && defined(GC_DLL) || defined (_AIX)
    /*
     * Similarly gnu-win32 DLLs need explicit initialization from
     * the main program, as does AIX.
     */
#   define GC_INIT() { GC_add_roots(DATASTART, DATAEND); }
# else
#  if defined(__APPLE__) && defined(__MACH__) || defined(GC_WIN32_THREADS)
#   define GC_INIT() { GC_init(); }
#  else
#   define GC_INIT()
#  endif /* !__MACH && !GC_WIN32_THREADS */
# endif /* !AIX && !cygwin */
#endif /* !sparc */

#if !defined(_WIN32_WCE) \
    && ((defined(_MSDOS) || defined(_MSC_VER)) && (_M_IX86 >= 300) \
        || defined(_WIN32) && !defined(__CYGWIN32__) && !defined(__CYGWIN__))
  /* win32S may not free all resources on process exit.  */
  /* This explicitly deallocates the heap.		 */
    GC_API void GC_win32_free_heap ();
#endif

#if ( defined(_AMIGA) && !defined(GC_AMIGA_MAKINGLIB) )
  /* Allocation really goes through GC_amiga_allocwrapper_do */
# include "gc_amiga_redirects.h"
#endif

#if defined(GC_REDIRECT_TO_LOCAL) && !defined(GC_LOCAL_ALLOC_H)
#  include  "gc_local_alloc.h"
#endif

#ifdef __cplusplus
    }  /* end of extern "C" */
#endif

#endif /* _GC_H */
