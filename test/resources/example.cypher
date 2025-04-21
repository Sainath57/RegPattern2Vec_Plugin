CREATE (N1:type1 {name:'a1', label:'label1'})
CREATE (N2:type1 {name:'a2', label:'label2'})
CREATE (N3:type1 {name:'a3', label:'label3'})
CREATE (N4:type1 {name:'a4', label:'label4'})
CREATE (N5:type2 {name:'b1', label:'label5'})
CREATE (N6:type3 {name:'c1', label:'label6'})
CREATE (N7:type3 {name:'c2', label:'label7'})
CREATE (N8:type3 {name:'c3', label:'label8'})
CREATE (N9:type4 {name:'d1', label:'label9'})
CREATE (N10:type4 {name:'d2', label:'label10'})
CREATE (N11:type4 {name:'d3', label:'label11'})
CREATE
  (N1)-[:R1 {type:'aType'}]->(N6),
  (N1)-[:R4 {type:'dType'}]->(N5),
  (N5)-[:R4 {type:'dType'}]->(N2),
  (N6)-[:R1 {type:'aType'}]->(N2),
  (N6)-[:R3 {type:'cType'}]->(N7),
  (N2)-[:R2 {type:'bType'}]->(N9),
  (N7)-[:R1 {type:'aType'}]->(N3),
  (N3)-[:R2 {type:'bType'}]->(N10),
  (N7)-[:R3 {type:'cType'}]->(N8),
  (N8)-[:R1 {type:'aType'}]->(N4),
  (N4)-[:R2 {type:'bType'}]->(N11)