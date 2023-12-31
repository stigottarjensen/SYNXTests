select bygning_navn, bygnings_id, eiendom, eiendom_intern_nr, objekt_type, objekt_tekst,[Plassering],[Type slokkemiddel],[Stralerør_extracolumn]
from (
select bygning_navn , rtrim(BBO_Navn) as eiendom, 
        rtrim(OBT_Objekt_Type.OBT_Type) as objekt_type, 
        rtrim(bygning_ref_nr) as bygnings_id,
        rtrim(BBO_InternNr) as eiendom_intern_nr,
        rtrim(OBT_Tekst) as objekt_tekst, 
        rtrim(OBF_Label)+ CASE WHEN trim(OBF_Type_Felt)='combobox' THEN '_extracolumn' ELSE '' END AS obflabel, 
        rtrim(OBV_Verdi_Tekst) + CASE WHEN trim(OBF_Type_Felt)='combobox' THEN '-@-@-'+ rtrim(ISNULL(BVV_Tekst,'')) ELSE '' END  as vete
    FROM view_bygning
        inner join BBO_Brannobjekt on view_bygning.object_nr=BBO_Brannobjekt.BBO_Nr
        inner join OBJ_Objekt on view_bygning.bygning_nr=OBJ_Objekt.BBY_Nr and BBO_Brannobjekt.BBO_Nr=OBJ_Objekt.BBO_Nr
        inner join OBT_Objekt_Type on OBJ_Objekt.OBT_Type=OBT_Objekt_Type.OBT_Type and OBJ_Objekt.BFO_Nr=OBT_Objekt_Type.BFO_Nr
        inner join OBF_Objekt_Felt on OBT_Objekt_Type.OBT_Type= OBF_Objekt_Felt.OBT_Type
        inner join OBV_verdi on OBJ_Objekt.Obj_nr=OBV_Verdi.OBJ_Nr and OBF_Objekt_Felt.OBF_Nr=OBV_Verdi.OBF_Nr
         left join BVV_Valgbare_verdier bvv on OBF_Objekt_Felt.BVV_Type=bvv.BVV_Type and OBV_verdi.OBV_Verdi_Tekst=bvv.BVV_Kode and OBJ_Objekt.BFO_Nr=bvv.BFO_Nr
        ) as bygobj pivot 
  (max (vete)
  for obflabel IN ([Plassering],[Type slokkemiddel],[Stralerør_extracolumn])) as pvt
  order by pvt.objekt_type