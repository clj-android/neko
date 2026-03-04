(ns neko.ui.support
  "Convenience umbrella namespace that loads ALL AndroidX support library
  widgets.  Requiring this namespace registers every AndroidX widget
  with the mapping system.

  This requires all AndroidX dependencies to be on the classpath.  If
  your app only uses some widgets, require the individual namespaces
  instead (e.g. `neko.ui.support.material` for TabLayout/FAB).

  Individual namespaces (each needs only its corresponding Maven dependency):
    neko.ui.support.material          — TabLayout, FAB, AppBarLayout
    neko.ui.support.recycler-view     — RecyclerView
    neko.ui.support.card-view         — CardView
    neko.ui.support.toolbar           — Toolbar
    neko.ui.support.drawer-layout     — DrawerLayout
    neko.ui.support.view-pager        — ViewPager
    neko.ui.support.swipe-refresh     — SwipeRefreshLayout
    neko.ui.support.coordinator-layout — CoordinatorLayout, NestedScrollView"
  (:require neko.ui.support.material
            neko.ui.support.recycler-view
            neko.ui.support.card-view
            neko.ui.support.toolbar
            neko.ui.support.drawer-layout
            neko.ui.support.view-pager
            neko.ui.support.swipe-refresh
            neko.ui.support.coordinator-layout))
