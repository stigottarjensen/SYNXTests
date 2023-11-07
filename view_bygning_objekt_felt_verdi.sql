select OBF_Label FROM OBF_Objekt_Felt ORDER BY OBF_Nr

select BBY_Navn, [Plassering], [Etasje], [Rom nummer], [Produksjonsår], [Lengde], [Dimensjon], [Siste kontroll], [Neste kontroll]
from (
select BBY_Navn , OBF_Label, rtrim(OBV_Verdi_Tekst) as vete
    FROM [Abisair].[dbo].[BBY_Bygning]
        inner join BBO_Brannobjekt on BBY_Bygning.BBO_Nr=BBO_Brannobjekt.BBO_Nr
        inner join OBJ_Objekt on BBY_Bygning.bby_nr=OBJ_Objekt.BBY_Nr and BBO_Brannobjekt.BBO_Nr=OBJ_Objekt.BBO_Nr
        inner join OBT_Objekt_Type on dbo.OBJ_Objekt.OBT_Type=dbo.OBT_Objekt_Type.OBT_Type and OBJ_Objekt.BFO_Nr=OBT_Objekt_Type.BFO_Nr
        inner join [dbo].[OBF_Objekt_Felt] on dbo.OBT_Objekt_Type.OBT_Type= dbo.OBF_Objekt_Felt.OBT_Type
        inner join OBV_verdi on OBJ_Objekt.Obj_nr=OBV_Verdi.OBJ_Nr and OBF_Objekt_Felt.OBF_Nr=OBV_Verdi.OBF_Nr)  as bygobj
pivot 
  (max (vete)
  for OBF_Label IN ([Plassering], [Etasje], [Rom nummer], [Produksjonsår], [Lengde], [Dimensjon], [Siste kontroll], [Neste kontroll])) as pvt
  order by pvt.BBY_Navn, pvt.OBJ_Navn