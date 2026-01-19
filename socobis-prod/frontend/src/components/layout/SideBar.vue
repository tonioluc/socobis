<!-- frontend/src/components/layout/SideBar.vue -->
<template>
  <aside class="sidebar">
    <!-- Header avec logo -->
    <div class="sidebar-header">
      <div class="flex items-center space-x-4">
        <div class="logo-container">
          <Flag class="w-7 h-7 text-white" />
        </div>
        <div class="flex-1">
          <h1 class="text-2xl font-bold text-white tracking-tight">Fabrication Management</h1>
          <p class="text-sm text-gray-400 font-medium">Gestion de Fabrication</p>
        </div>
      </div>
    </div>

    <!-- Navigation principale -->
    <nav class="sidebar-nav">
      <!-- Section Production -->
      <div class="nav-section">
        <h3 class="nav-section-title">
          <span class="flex items-center gap-2">
            <div class="w-1 h-4 bg-white rounded-full"></div>
            PRODUCTION
          </span>
        </h3>

        <!-- Ordre de fabrication -->
        <div class="submenu-section">
          <div class="submenu-header" @click="toggleSubmenu('orders')">
            <ClipboardList class="w-4 h-4 text-gray-400" />
            <span class="submenu-title">Ordre de fabrication</span>
            <ChevronDown class="w-4 h-4 text-gray-400 transition-transform" :class="{ 'rotate-180': expandedMenus.orders }" />
          </div>
          <div class="submenu-items" :class="{ 'expanded': expandedMenus.orders }">
            <router-link
              to="/ordre-fabrication/liste"
              class="nav-subitem"
              :class="{ 'active': isActive('/ordre-fabrication/liste') }"
            >
              <span class="nav-subitem-label">Liste</span>
            </router-link>
            <router-link
              to="/ordre-fabrication/saisie"
              class="nav-subitem"
              :class="{ 'active': isActive('/ordre-fabrication/saisie') }"
            >
              <span class="nav-subitem-label">Saisie</span>
            </router-link>
          </div>
        </div>

        <!-- Fabrication -->
        <div class="submenu-section">
          <div class="submenu-header" @click="toggleSubmenu('fabrication')">
            <Package class="w-4 h-4 text-gray-400" />
            <span class="submenu-title">Fabrication</span>
            <ChevronDown class="w-4 h-4 text-gray-400 transition-transform" :class="{ 'rotate-180': expandedMenus.fabrication }" />
          </div>
          <div class="submenu-items" :class="{ 'expanded': expandedMenus.fabrication }">
            <router-link
              to="/fabrication/liste"
              class="nav-subitem"
              :class="{ 'active': isActive('/fabrication/liste') }"
            >
              <span class="nav-subitem-label">Liste</span>
            </router-link>
            <router-link
              to="/fabrication/saisie"
              class="nav-subitem"
              :class="{ 'active': isActive('/fabrication/saisie') }"
            >
              <span class="nav-subitem-label">Saisie</span>
            </router-link>
          </div>
        </div>
      </div>
    </nav>

    <!-- Footer avec profil utilisateur -->
    <!-- <div class="sidebar-footer">
      <div class="user-card">
        <div class="user-avatar-container">
          <div class="user-avatar">
            <User class="w-6 h-6 text-gray-900" />
          </div>
          <div class="status-indicator"></div>
        </div>
        <div class="user-details">
          <p class="user-name">Administrateur</p>
          <p class="user-role">Super Admin</p>
        </div>
        <button class="logout-btn" @click="logout" title="Se déconnecter">
          <LogOut class="w-5 h-5" />
        </button>
      </div>
    </div> -->
  </aside>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Flag,
  Home,
  Package,
  LogOut,
  ChevronDown,
  ClipboardList,
  User
} from 'lucide-vue-next'

const route = useRoute()

const expandedMenus = ref({
  fabrication: false,
  orders: true // Ouvrir par défaut les ordres de fabrication
})

const toggleSubmenu = (menu) => {
  expandedMenus.value[menu] = !expandedMenus.value[menu]
}

const isActive = computed(() => (path) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
})

const logout = () => {
  localStorage.removeItem('token')
  window.location.href = '/login'
}
</script>

<style scoped>
/* Sidebar principale */
.sidebar {
  @apply fixed left-0 top-0 h-screen w-72 bg-gray-900 shadow-2xl flex flex-col z-50;
  background: linear-gradient(180deg, #111827 0%, #000000 100%);
}

/* Header */
.sidebar-header {
  @apply p-6 border-b border-gray-800;
}

.logo-container {
  @apply w-14 h-14 bg-white rounded-2xl flex items-center justify-center shadow-xl;
  background: linear-gradient(135deg, #ffffff 0%, #f3f4f6 100%);
}

/* Navigation */
.sidebar-nav {
  @apply flex-1 overflow-y-auto py-6 px-4 space-y-8;
}

.sidebar-nav::-webkit-scrollbar {
  @apply w-1.5;
}

.sidebar-nav::-webkit-scrollbar-track {
  @apply bg-gray-900;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  @apply bg-gray-700 rounded-full;
}

.sidebar-nav::-webkit-scrollbar-thumb:hover {
  @apply bg-gray-600;
}

/* Section de navigation */
.nav-section {
  @apply space-y-2;
}

.nav-section-title {
  @apply text-xs font-bold text-gray-400 uppercase tracking-wider px-4 mb-4;
}

/* Groupe de navigation (parent + enfants) */
.nav-group {
  @apply space-y-1;
}

/* Items de navigation */
.nav-item {
  @apply flex items-center space-x-3 px-4 py-3.5 rounded-xl;
  @apply text-gray-400 hover:text-white cursor-pointer relative;
  @apply overflow-hidden transition-all;
}

.nav-item::before {
  content: '';
  @apply absolute inset-0 bg-white transition-opacity;
  opacity: 0;
}

.nav-item:hover::before {
  opacity: 0.05;
}

.nav-item-icon {
  @apply relative z-10 flex items-center justify-center;
  @apply w-10 h-10 rounded-lg bg-gray-800 transition-all;
}

.nav-item:hover .nav-item-icon {
  @apply bg-gray-700;
}

.nav-item.active .nav-item-icon {
  @apply bg-white text-gray-900;
}

.nav-item-label {
  @apply relative z-10 font-semibold text-sm flex-1;
}

.nav-item.active {
  @apply text-white bg-gray-800;
}

.nav-item.active::after {
  content: '';
  @apply absolute right-0 top-1/2 -translate-y-1/2 w-1 h-8 bg-white rounded-l-full;
}

.nav-item-indicator {
  @apply ml-auto w-2 h-2 rounded-full bg-gray-700 transition-all;
  opacity: 0;
}

.nav-item:hover .nav-item-indicator {
  opacity: 1;
}

.nav-item.active .nav-item-indicator {
  @apply bg-white;
  opacity: 1;
}

/* Sous-menus */
.submenu-section {
  @apply mb-4;
}

.submenu-header {
  @apply flex items-center gap-3 px-4 py-3 text-gray-300 hover:text-white hover:bg-gray-800 rounded-lg cursor-pointer transition-all duration-200;
}

.submenu-title {
  @apply text-sm font-medium flex-1;
}

.submenu-items {
  @apply overflow-hidden transition-all duration-300 ease-in-out;
  max-height: 0;
}

.submenu-items.expanded {
  max-height: 200px;
}

.nav-subitem {
  @apply flex items-center px-8 py-2.5 text-sm text-gray-400 hover:text-white hover:bg-gray-800 rounded-lg mx-2 my-1 transition-all duration-200;
  position: relative;
}

.nav-subitem.active {
  @apply text-white bg-gray-800;
}

.nav-subitem.active::before {
  content: '';
  @apply absolute left-0 top-0 bottom-0 w-1 bg-blue-500 rounded-r;
}

/* Footer */
.sidebar-footer {
  @apply p-4 border-t border-gray-800;
}

.user-card {
  @apply flex items-center space-x-4 p-4 rounded-xl;
  @apply bg-gray-800 hover:bg-gray-700 cursor-pointer;
  @apply transition-all;
}

.user-avatar-container {
  @apply relative;
}

.user-avatar {
  @apply w-12 h-12 rounded-xl bg-white flex items-center justify-center shadow-lg;
}

.status-indicator {
  @apply absolute -bottom-1 -right-1 w-4 h-4;
  @apply bg-green-500 border-2 border-gray-900 rounded-full;
}

.user-details {
  @apply flex-1;
}

.user-name {
  @apply text-sm font-bold text-white;
}

.user-role {
  @apply text-xs text-gray-400 font-medium;
}

.logout-btn {
  @apply p-2.5 rounded-lg bg-gray-900 text-gray-400;
  @apply hover:bg-red-600 hover:text-white;
  @apply transition-all shadow-md;
}

.user-card:hover .logout-btn {
  @apply scale-110;
}

/* Responsive */
@media (max-width: 1024px) {
  .sidebar {
    @apply w-20;
  }

  .sidebar-header,
  .user-details,
  .nav-item-label,
  .nav-section-title,
  .badge-notification {
    @apply hidden;
  }

  .nav-item,
  .nav-subitem {
    @apply justify-center px-2;
  }

  .nav-item.active::after {
    @apply hidden;
  }

  .user-card {
    @apply justify-center space-x-0;
  }

  .logout-btn {
    @apply hidden;
  }
}
</style>