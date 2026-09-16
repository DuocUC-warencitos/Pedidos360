import tsParser from '@typescript-eslint/parser';
import tsPlugin from '@typescript-eslint/eslint-plugin';
import importPlugin from 'eslint-plugin-import';
import simpleImportSort from 'eslint-plugin-simple-import-sort';

/** @type {import('eslint').Linter.FlatConfig[]} */
export default [
  {
    ignores: ['dist/**', 'node_modules/**', '.angular/**', 'out-tsc/**'],
  },
  {
    files: ['**/*.ts'],
    languageOptions: {
      parser: tsParser,
      parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module',
      },
    },
    plugins: {
      '@typescript-eslint': tsPlugin,
      import: importPlugin,
      'simple-import-sort': simpleImportSort,
    },
    settings: {
      'import/resolver': {
        typescript: {
          project: ['./tsconfig.json', './tsconfig.app.json'],
        },
        node: true,
      },
    },
    rules: {
      // Orden canónico: Angular → libs externas → alias internos → relativos
      'import/order': [
        'warn',
        {
          groups: ['builtin', 'external', 'internal', 'parent', 'sibling', 'index', 'object', 'type'],
          pathGroups: [
            { pattern: '@angular/**', group: 'external', position: 'before' },
            { pattern: '@azure/**', group: 'external', position: 'before' },
            { pattern: '@tanstack/**', group: 'external', position: 'before' },
            { pattern: 'rxjs', group: 'external', position: 'before' },
            { pattern: '@core/**', group: 'internal', position: 'before' },
            { pattern: '@shared/**', group: 'internal', position: 'before' },
            { pattern: '@features/**', group: 'internal', position: 'before' },
            { pattern: '@env/**', group: 'internal', position: 'before' },
          ],
          pathGroupsExcludedImportTypes: ['builtin'],
          distinctGroup: false,
          'newlines-between': 'always',
          alphabetize: { order: 'asc', caseInsensitive: true },
        },
      ],
      'simple-import-sort/imports': 'off',
      'simple-import-sort/exports': 'warn',
      // Evitar imports relativos profundos frágiles (sugerir alias)
      'no-restricted-imports': [
        'warn',
        {
          patterns: [
            {
              group: ['../../*'],
              message: 'Usa alias @core/@shared/@features/@env en lugar de ../../',
            },
          ],
        },
      ],
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    },
  },
];
