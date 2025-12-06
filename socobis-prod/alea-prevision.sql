ALTER TABLE VENTE
ADD planpaiement varchar(200);

commit;

CREATE OR REPLACE VIEW INSERTION_VENTE AS
SELECT
	v.ID,
	v.DESIGNATION,
	v.IDMAGASIN,
	v.DATY,
	v.REMARQUE,
	v.ETAT,
	v.IDORIGINE,
	v.IDCLIENT,
	CAST(' ' AS varchar(100)) AS iddevise,
	v.ESTPREVU,
	v.DATYPREVU,
	v.IDRESERVATION,
	v.echeancefacture,
	v.modepaiement,
	v.modelivraison,
	v.fraislivraison,
	v.referencefact,
    v.planpaiement
FROM VENTE v;

commit;
