<!-- frontend/src/App.vue -->
<template>
  <div class="app-container">
    <SideBar />
    <main class="main-content">
      <div class="content-wrapper">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup>
import SideBar from './components/layout/SideBar.vue'
</script>

<style>
/* Import de la police Inter pour un rendu professionnel */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800;900&display=swap');

* {
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
}

.app-container {
  @apply flex min-h-screen bg-gray-50;
}

.main-content {
  @apply flex-1 ml-72 min-h-screen;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
}

.content-wrapper {
  @apply p-8 max-w-[1600px] mx-auto;
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* Responsive */
@media (max-width: 1024px) {
  .main-content {
    @apply ml-20;
  }
  
  .content-wrapper {
    @apply p-6;
  }
}

@media (max-width: 768px) {
  .main-content {
    @apply ml-0;
  }
  
  .content-wrapper {
    @apply p-4;
  }
}

/* Styles globaux pour améliorer l'apparence */
h1, h2, h3, h4, h5, h6 {
  @apply font-bold tracking-tight;
}

button {
  @apply focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-900;
}

input:focus,
select:focus,
textarea:focus {
  @apply outline-none;
}

/* Animation pour les éléments qui apparaissent */
@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-slide-up {
  animation: slideUp 0.4s ease-out;
}

/* Effet de survol amélioré pour les cartes */
.card-hover {
  @apply transition-all duration-300 hover:shadow-2xl hover:-translate-y-1;
}

/* Badge amélioré */
.modern-badge {
  @apply inline-flex items-center px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide;
}

/* Bouton avec effet de brillance */
.btn-shine {
  @apply relative overflow-hidden;
}

.btn-shine::before {
  content: '';
  @apply absolute top-0 left-0 w-full h-full bg-gradient-to-r from-transparent via-white to-transparent opacity-0;
  transform: translateX(-100%);
  transition: all 0.6s ease;
}

.btn-shine:hover::before {
  @apply opacity-20;
  transform: translateX(100%);
}
</style>