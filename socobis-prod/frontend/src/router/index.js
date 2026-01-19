import { createRouter, createWebHistory } from "vue-router";
import Dashboard from "../views/Dashboard.vue";
import OrdreFabricationSaisie from "../views/OrdreFabricationSaisie.vue";
import OrdreFabricationFiche from "../views/OrdreFabricationFiche.vue";
import OrdreFabricationListe from "../views/OrdreFabricationListe.vue";
import FabricationSaisie from "../views/FabricationSaisie.vue";
import FabricationFiche from "../views/FabricationFiche.vue";
import FabricationListe from "../views/FabricationListe.vue";
import MvtStockSaisie from "../views/MvtStockSaisie.vue";
import MvtStockFiche from "../views/MvtStockFiche.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: "/", name: "dashboard", component: Dashboard },
    { path: "/ordre-fabrication/saisie", name: "ordre-fabrication-saisie", component: OrdreFabricationSaisie },
    { path: "/ordre-fabrication/fiche/:id", name: "ordre-fabrication-fiche", component: OrdreFabricationFiche },
    { path: "/ordre-fabrication/liste", name: "ordre-fabrication-liste", component: OrdreFabricationListe },
    { path: "/fabrication/saisie", name: "fabrication-saisie", component: FabricationSaisie },
    { path: "/fabrication/fiche/:id", name: "fabrication-fiche", component: FabricationFiche },
    { path: "/fabrication/liste", name: "fabrication-liste", component: FabricationListe },
    { path: "/stock/mvtstock/saisie", name: "mvtstock-saisie", component: MvtStockSaisie },
    { path: "/stock/mvtstock/fiche/:id", name: "mvtstock-fiche", component: MvtStockFiche },
  ],
});

export default router;
