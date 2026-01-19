<template>
  <div class="autocomplete-container" :class="{ 'has-error': hasError }">
    <div class="input-wrapper">
      <input
        ref="inputRef"
        :value="displayValue"
        @input="onInput"
        @focus="onFocus"
        @blur="onBlur"
        @keydown="onKeydown"
        :placeholder="placeholder"
        :class="inputClass"
        :disabled="disabled"
        :name="name"
        autocomplete="off"
      />
      <div v-if="isLoading" class="loading-indicator">
        <div class="spinner"></div>
      </div>
      <button
        v-if="showClearButton && modelValue"
        @click="clearSelection"
        type="button"
        class="clear-button"
      >
        ×
      </button>
    </div>

    <!-- Dropdown -->
    <div
      v-if="showDropdown"
      class="dropdown"
      :class="{ 'dropdown--above': dropdownPosition === 'above' }"
    >
      <div v-if="filteredResults.length === 0 && !isLoading" class="no-results">
        Aucun résultat trouvé
      </div>
      <div
        v-for="(item, index) in filteredResults"
        :key="getItemKey(item)"
        @mousedown="selectItem(item)"
        class="dropdown-item"
        :class="{ 'dropdown-item--highlighted': highlightedIndex === index }"
      >
        <span class="item-label">{{ getItemDisplay(item) }}</span>
        <span v-if="showSelectedBadge && isSelected(item)" class="selected-badge">
          ✓
        </span>
      </div>
    </div>

    <!-- Selected item badge (when showSelectedBadge is true) -->
    <div v-if="showSelectedBadge && selectedItem" class="selected-item-badge">
      <span class="badge-label">{{ getItemDisplay(selectedItem) }}</span>
      <button @click="clearSelection" type="button" class="badge-remove">
        ×
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number, Object],
    default: null
  },
  searchFunction: {
    type: Function,
    required: true
  },
  allItems: {
    type: Array,
    default: null
  },
  valueField: {
    type: String,
    default: 'value'
  },
  displayField: {
    type: String,
    default: 'label'
  },
  placeholder: {
    type: String,
    default: 'Rechercher...'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  minQueryLength: {
    type: Number,
    default: 1
  },
  maxResults: {
    type: Number,
    default: 10
  },
  debounceMs: {
    type: Number,
    default: 300
  },
  showSelectedBadge: {
    type: Boolean,
    default: false
  },
  showClearButton: {
    type: Boolean,
    default: true
  },
  inputClass: {
    type: String,
    default: ''
  },
  name: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'select', 'clear'])

const inputRef = ref(null)
const query = ref('')
const results = ref([])
const isLoading = ref(false)
const showDropdown = ref(false)
const highlightedIndex = ref(-1)
const dropdownPosition = ref('below')
const selectedItem = ref(null)

let debounceTimer = null

// Computed
const displayValue = computed(() => {
  if (selectedItem.value) {
    return getItemDisplay(selectedItem.value)
  }
  return query.value
})

const filteredResults = computed(() => {
  return results.value.slice(0, props.maxResults)
})

const hasError = computed(() => {
  return false // Pour extension future
})

// Methods
const getItemKey = (item) => {
  return item[props.valueField] || item.id || item.value || Math.random()
}

const getItemValue = (item) => {
  return item[props.valueField] || item.value || item.id
}

const getItemDisplay = (item) => {
  const display = item[props.displayField] || item.label || item.nom || item.designation || item.libelle || item.val || item.id || ''
  if (!display) {
    // Afficher TOUTES les propriétés pour debugger
    const allProps = {}
    Object.keys(item).forEach(key => {
      allProps[key] = item[key]
    })
    console.error('⚠️ [AutoComplete] Aucun display trouvé, voici TOUS les champs:', allProps)
  }
  return display
}

const isSelected = (item) => {
  const itemValue = getItemValue(item)
  return itemValue === props.modelValue
}

const onInput = async (event) => {
  const value = event.target.value
  query.value = value
  selectedItem.value = null
  emit('update:modelValue', null)

  if (value.length >= props.minQueryLength) {
    await performSearch(value)
  } else {
    results.value = []
    showDropdown.value = false
  }
}

const performSearch = async (searchQuery) => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }

  debounceTimer = setTimeout(async () => {
    isLoading.value = true
    try {
      const searchResults = await props.searchFunction(searchQuery)
      results.value = searchResults
      showDropdown.value = true
      highlightedIndex.value = -1
      calculateDropdownPosition()
    } catch (error) {
      console.error('Erreur lors de la recherche:', error)
      results.value = []
    } finally {
      isLoading.value = false
    }
  }, props.debounceMs)
}

const onFocus = () => {
  if (results.value.length > 0) {
    showDropdown.value = true
    calculateDropdownPosition()
  }
}

const onBlur = () => {
  // Délai pour permettre la sélection d'un élément
  setTimeout(() => {
    showDropdown.value = false
  }, 200)
}

const onKeydown = (event) => {
  if (!showDropdown.value) return

  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      highlightedIndex.value = Math.min(highlightedIndex.value + 1, filteredResults.value.length - 1)
      break
    case 'ArrowUp':
      event.preventDefault()
      highlightedIndex.value = Math.max(highlightedIndex.value - 1, -1)
      break
    case 'Enter':
      event.preventDefault()
      if (highlightedIndex.value >= 0) {
        selectItem(filteredResults.value[highlightedIndex.value])
      }
      break
    case 'Escape':
      showDropdown.value = false
      highlightedIndex.value = -1
      break
  }
}

const selectItem = (item) => {
  selectedItem.value = item
  query.value = ''
  showDropdown.value = false
  highlightedIndex.value = -1

  const value = getItemValue(item)
  emit('update:modelValue', value)
  emit('select', item)
}

const clearSelection = () => {
  selectedItem.value = null
  query.value = ''
  results.value = []
  showDropdown.value = false
  emit('update:modelValue', null)
  emit('clear')
}

const calculateDropdownPosition = () => {
  nextTick(() => {
    if (!inputRef.value) return

    const rect = inputRef.value.getBoundingClientRect()
    const viewportHeight = window.innerHeight
    const dropdownHeight = 200 // Estimation

    if (rect.bottom + dropdownHeight > viewportHeight && rect.top > dropdownHeight) {
      dropdownPosition.value = 'above'
    } else {
      dropdownPosition.value = 'below'
    }
  })
}

// Fonction helper pour trouver un item par sa valeur
const findItemByValue = async (value) => {
  console.log('🔍 [AutoComplete] Recherche de l\'item pour la valeur:', value)
  
  // Si allItems est fourni, chercher directement dedans (plus rapide et complet)
  if (props.allItems && props.allItems.length > 0) {
    console.log('📋 [AutoComplete] Recherche dans allItems:', props.allItems.length, 'items')
    const matchedItem = props.allItems.find(item => getItemValue(item) === value)
    if (matchedItem) {
      console.log('✅ [AutoComplete] Item trouvé dans allItems:', matchedItem)
      return matchedItem
    }
  }
  
  // Sinon, utiliser la fonction de recherche
  try {
    const searchResults = await props.searchFunction('')
    console.log('📋 [AutoComplete] Résultats de searchFunction:', searchResults.length)
    const matchedItem = searchResults.find(item => getItemValue(item) === value)
    if (matchedItem) {
      console.log('✅ [AutoComplete] Item trouvé via searchFunction:', matchedItem)
      return matchedItem
    }
  } catch (error) {
    console.error('❌ [AutoComplete] Erreur lors de la recherche:', error)
  }
  
  console.log('⚠️ [AutoComplete] Aucun item trouvé pour la valeur:', value)
  return null
}

// Watch for external model changes
watch(() => props.modelValue, async (newValue, oldValue) => {
  console.log('🔔 [AutoComplete] modelValue changé:', oldValue, '->', newValue)
  if (newValue === null || newValue === '') {
    selectedItem.value = null
    query.value = ''
  } else if (newValue !== oldValue && !selectedItem.value) {
    // Une nouvelle valeur a été assignée de l'extérieur
    const item = await findItemByValue(newValue)
    if (item) {
      selectedItem.value = item
    }
  }
})

// Initialize
onMounted(async () => {
  // Si une valeur initiale est fournie, chercher l'item correspondant
  if (props.modelValue) {
    console.log('🚀 [AutoComplete] Valeur initiale détectée:', props.modelValue)
    const item = await findItemByValue(props.modelValue)
    if (item) {
      selectedItem.value = item
    }
  }
})
</script>

<style scoped>
.autocomplete-container {
  @apply relative;
}

.input-wrapper {
  @apply relative;
}

.input-wrapper input {
  @apply w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent;
  background-color: white !important;
}

.input-wrapper input:disabled {
  @apply bg-gray-100 cursor-not-allowed;
}

.loading-indicator {
  @apply absolute right-8 top-1/2 transform -translate-y-1/2;
}

.spinner {
  @apply w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin;
}

.clear-button {
  @apply absolute right-2 top-1/2 transform -translate-y-1/2 w-6 h-6 flex items-center justify-center text-gray-400 hover:text-gray-600 rounded-full hover:bg-gray-100;
}

.dropdown {
  @apply absolute z-50 w-full bg-white border border-gray-300 rounded-lg shadow-lg max-h-60 overflow-y-auto;
  top: 100%;
  margin-top: 2px;
}

.dropdown--above {
  top: auto;
  bottom: 100%;
  margin-top: 0;
  margin-bottom: 2px;
}

.dropdown-item {
  @apply px-3 py-2 cursor-pointer hover:bg-gray-100 flex justify-between items-center;
}

.dropdown-item--highlighted {
  @apply bg-blue-50;
}

.item-label {
  @apply flex-1;
}

.selected-badge {
  @apply text-green-600 font-bold ml-2;
}

.no-results {
  @apply px-3 py-4 text-gray-500 text-center;
}

.selected-item-badge {
  @apply inline-flex items-center gap-2 px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm mt-2;
}

.badge-remove {
  @apply w-4 h-4 flex items-center justify-center text-blue-600 hover:text-blue-800 hover:bg-blue-200 rounded-full;
}

.has-error .input-wrapper input {
  @apply border-red-300 focus:ring-red-500;
}
</style>