CREATE OR REPLACE VIEW RAPPROCHEMENT_BF_CLIENT_2 AS
SELECT
    abcc.ID,
    abcc.PRODUIT,
    abcc.NUMBL,
    abcc.QUANTITE,
    abcc.IDVENTEDETAIL,
    abcc.UNITE,
    abcc.IDBC_FILLE,
    abcc.IDPRODUITLIB,
    abcc.IDVENTE,
    abcc.IDBC,
    abcc.QTELIVREE,
    abcc.QTERESTEALIVRER,
    COALESCE(m.qteSortie, 0) AS qtsSortie,
    abcc.QTERESTEALIVRER - abcc.QTELIVREE AS resteASortir
FROM AS_BONLIVRFILLE_CLIENT_CPL abcc
         LEFT JOIN SORTIEPRODUIT m ON m.idproduit = abcc.PRODUIT
         LEFT JOIN MVTSTOCK mv ON mv.IDOBJET = abcc.NUMBL;
