// Gera src/main/resources/catalogo/exercicios.json a partir do free-exercise-db (Unlicense)
// e dos nomes em portugues de tools/catalogo/traducoes-*.txt (linhas "Nome em ingles | Nome em portugues").
//
// Uso (na raiz do trainingapp-api):
//   node tools/catalogo/gerar-catalogo.mjs [caminho/para/exercises.json]
// Sem argumento, baixa o dataset do GitHub.

import { readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const URL_DATASET = 'https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json';
const aqui = dirname(fileURLToPath(import.meta.url));
const saida = join(aqui, '..', '..', 'src', 'main', 'resources', 'catalogo', 'exercicios.json');

const GRUPO = {
  abdominals: 'ABDOMEN',
  hamstrings: 'POSTERIORES',
  adductors: 'ADUTORES',
  abductors: 'ABDUTORES',
  quadriceps: 'QUADRICEPS',
  biceps: 'BICEPS',
  triceps: 'TRICEPS',
  shoulders: 'OMBROS',
  chest: 'PEITO',
  'middle back': 'COSTAS',
  lats: 'COSTAS',
  'lower back': 'LOMBAR',
  traps: 'TRAPEZIO',
  calves: 'PANTURRILHA',
  glutes: 'GLUTEOS',
  forearms: 'ANTEBRACO',
  neck: 'PESCOCO',
};

const EQUIPAMENTO = {
  barbell: 'BARRA',
  'e-z curl bar': 'BARRA',
  dumbbell: 'HALTERES',
  machine: 'MAQUINA',
  cable: 'CABO',
  'body only': 'PESO_CORPORAL',
  kettlebells: 'KETTLEBELL',
  bands: 'ELASTICO',
  other: 'OUTRO',
  'foam roll': 'OUTRO',
  'medicine ball': 'OUTRO',
  'exercise ball': 'OUTRO',
};

function lerTraducoes() {
  const mapa = new Map();
  const arquivos = readdirSync(aqui).filter((f) => /^traducoes-.*\.txt$/.test(f)).sort();
  for (const arquivo of arquivos) {
    readFileSync(join(aqui, arquivo), 'utf8').split(/\r?\n/).forEach((linha, i) => {
      if (!linha.trim()) return;
      const partes = linha.split(' | ');
      if (partes.length !== 2) throw new Error(`${arquivo}:${i + 1}: formato invalido: ${linha}`);
      const [en, pt] = partes.map((p) => p.trim());
      if (mapa.has(en)) throw new Error(`${arquivo}:${i + 1}: traducao duplicada para "${en}"`);
      mapa.set(en, pt);
    });
  }
  return mapa;
}

async function lerDataset() {
  const caminho = process.argv[2];
  if (caminho) return JSON.parse(readFileSync(caminho, 'utf8'));
  const resposta = await fetch(URL_DATASET);
  if (!resposta.ok) throw new Error(`Falha ao baixar o dataset: HTTP ${resposta.status}`);
  return resposta.json();
}

const dataset = await lerDataset();
const traducoes = lerTraducoes();
const erros = [];

const nomesEn = new Set(dataset.map((e) => e.name));
for (const e of dataset) if (!traducoes.has(e.name)) erros.push(`sem traducao: ${e.name}`);
for (const en of traducoes.keys()) if (!nomesEn.has(en)) erros.push(`traducao sem exercicio no dataset: ${en}`);

const catalogo = dataset.map((e) => {
  const musculo = e.primaryMuscles[0];
  const grupo = e.category === 'cardio' ? 'CARDIO' : GRUPO[musculo];
  const equipamento = e.equipment == null ? 'PESO_CORPORAL' : EQUIPAMENTO[e.equipment];
  if (!grupo) erros.push(`musculo sem mapeamento: ${musculo} (${e.id})`);
  if (!equipamento) erros.push(`equipamento sem mapeamento: ${e.equipment} (${e.id})`);
  return { slug: e.id, nome: traducoes.get(e.name), grupoMuscular: grupo, equipamento };
});

const porNome = new Map();
for (const item of catalogo) porNome.set(item.nome, [...(porNome.get(item.nome) ?? []), item.slug]);
for (const [nome, slugs] of porNome) if (slugs.length > 1) erros.push(`nome repetido "${nome}": ${slugs.join(', ')}`);

if (erros.length) {
  console.error(erros.join('\n'));
  console.error(`\n${erros.length} problema(s); nada foi gravado.`);
  process.exit(1);
}

catalogo.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR') || a.slug.localeCompare(b.slug));
const linhas = catalogo.map((item) => '  ' + JSON.stringify(item));
writeFileSync(saida, '[\n' + linhas.join(',\n') + '\n]\n', 'utf8');
console.log(`${catalogo.length} exercicios gravados em ${saida}`);
