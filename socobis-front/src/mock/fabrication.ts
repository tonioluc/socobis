export type ProductType = 'MATIERE_PREMIERE' | 'INTERMEDIAIRE' | 'FINI' | 'CHARGE'

export type Unit = 'kg' | 'g' | 'L' | 'u'

export type StockStatus = 'OK' | 'BAS' | 'RUPTURE'

export interface Product {
  id: string
  name: string
  type: ProductType
  unit: Unit
  stock: number
  stockStatus: StockStatus
}

export interface FormulaItem {
  itemId: string
  name: string
  type: ProductType
  unit: Unit
  qtyPerUnit: number
}

export interface ManufactureHistoryItem {
  id: string
  date: string
  productName: string
  qtyProduced: number
  unit: Unit
  status: 'TERMINEE' | 'EN_COURS' | 'ANNULEE'
}

export const products: Product[] = [
  // Produits finis (biscuit / bonbons)
  {
    id: 'PF_BISCUIT_CHOCO_100G',
    name: 'Biscuit chocolat 100g',
    type: 'FINI',
    unit: 'u',
    stock: 240,
    stockStatus: 'OK',
  },
  {
    id: 'PF_BONBON_MENTHE_10G',
    name: 'Bonbon menthe 10g',
    type: 'FINI',
    unit: 'u',
    stock: 80,
    stockStatus: 'BAS',
  },

  // Produits intermédiaires
  {
    id: 'PI_PATE_BISCUIT',
    name: 'Pâte biscuit (intermédiaire)',
    type: 'INTERMEDIAIRE',
    unit: 'kg',
    stock: 6.2,
    stockStatus: 'OK',
  },
  {
    id: 'PI_SIROP_MENTHE',
    name: 'Sirop menthe (intermédiaire)',
    type: 'INTERMEDIAIRE',
    unit: 'L',
    stock: 0.8,
    stockStatus: 'BAS',
  },

  // Matières premières
  {
    id: 'MP_FARINE',
    name: 'Farine',
    type: 'MATIERE_PREMIERE',
    unit: 'kg',
    stock: 55,
    stockStatus: 'OK',
  },
  {
    id: 'MP_SUCRE',
    name: 'Sucre',
    type: 'MATIERE_PREMIERE',
    unit: 'kg',
    stock: 18,
    stockStatus: 'BAS',
  },
  {
    id: 'MP_CACAO',
    name: 'Cacao',
    type: 'MATIERE_PREMIERE',
    unit: 'kg',
    stock: 3.5,
    stockStatus: 'BAS',
  },
  {
    id: 'MP_EMBALLAGE_SACHET',
    name: 'Sachet (emballage)',
    type: 'MATIERE_PREMIERE',
    unit: 'u',
    stock: 1200,
    stockStatus: 'OK',
  },
  {
    id: 'MP_CARTON',
    name: 'Carton',
    type: 'MATIERE_PREMIERE',
    unit: 'u',
    stock: 40,
    stockStatus: 'BAS',
  },

  // Charges
  {
    id: 'CH_ELECTRICITE',
    name: 'Électricité (charge)',
    type: 'CHARGE',
    unit: 'u',
    stock: 0,
    stockStatus: 'OK',
  },
]

export const formulasByProductId: Record<string, FormulaItem[]> = {
  PF_BISCUIT_CHOCO_100G: [
    {
      itemId: 'PI_PATE_BISCUIT',
      name: 'Pâte biscuit (intermédiaire)',
      type: 'INTERMEDIAIRE',
      unit: 'kg',
      qtyPerUnit: 0.08,
    },
    {
      itemId: 'MP_CACAO',
      name: 'Cacao',
      type: 'MATIERE_PREMIERE',
      unit: 'kg',
      qtyPerUnit: 0.003,
    },
    {
      itemId: 'MP_EMBALLAGE_SACHET',
      name: 'Sachet (emballage)',
      type: 'MATIERE_PREMIERE',
      unit: 'u',
      qtyPerUnit: 1,
    },
    {
      itemId: 'MP_CARTON',
      name: 'Carton',
      type: 'MATIERE_PREMIERE',
      unit: 'u',
      qtyPerUnit: 0.05,
    },
    {
      itemId: 'CH_ELECTRICITE',
      name: 'Électricité (charge)',
      type: 'CHARGE',
      unit: 'u',
      qtyPerUnit: 0.01,
    },
  ],

  PF_BONBON_MENTHE_10G: [
    {
      itemId: 'PI_SIROP_MENTHE',
      name: 'Sirop menthe (intermédiaire)',
      type: 'INTERMEDIAIRE',
      unit: 'L',
      qtyPerUnit: 0.005,
    },
    {
      itemId: 'MP_SUCRE',
      name: 'Sucre',
      type: 'MATIERE_PREMIERE',
      unit: 'kg',
      qtyPerUnit: 0.007,
    },
    {
      itemId: 'MP_EMBALLAGE_SACHET',
      name: 'Sachet (emballage)',
      type: 'MATIERE_PREMIERE',
      unit: 'u',
      qtyPerUnit: 1,
    },
    {
      itemId: 'CH_ELECTRICITE',
      name: 'Électricité (charge)',
      type: 'CHARGE',
      unit: 'u',
      qtyPerUnit: 0.01,
    },
  ],
}

export const history: ManufactureHistoryItem[] = [
  {
    id: 'FAB-2026-01-15-001',
    date: '2026-01-15 09:12',
    productName: 'Biscuit chocolat 100g',
    qtyProduced: 150,
    unit: 'u',
    status: 'TERMINEE',
  },
  {
    id: 'FAB-2026-01-16-002',
    date: '2026-01-16 14:40',
    productName: 'Bonbon menthe 10g',
    qtyProduced: 400,
    unit: 'u',
    status: 'TERMINEE',
  },
  {
    id: 'FAB-2026-01-18-003',
    date: '2026-01-18 10:05',
    productName: 'Biscuit chocolat 100g',
    qtyProduced: 80,
    unit: 'u',
    status: 'EN_COURS',
  },
]

export function formatProductType(t: ProductType): string {
  switch (t) {
    case 'MATIERE_PREMIERE':
      return 'Matière première'
    case 'INTERMEDIAIRE':
      return 'Intermédiaire'
    case 'FINI':
      return 'Produit fini'
    case 'CHARGE':
      return 'Charge'
  }
}

export function formatStockStatus(s: StockStatus): { label: string; tone: 'ok' | 'warn' | 'bad' } {
  switch (s) {
    case 'OK':
      return { label: 'OK', tone: 'ok' }
    case 'BAS':
      return { label: 'Bas', tone: 'warn' }
    case 'RUPTURE':
      return { label: 'Rupture', tone: 'bad' }
  }
}

export function round2(n: number): number {
  return Math.round(n * 100) / 100
}
