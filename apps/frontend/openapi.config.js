import { generateService } from '@umijs/openapi'

generateService({
  requestLibPath: "import request from '@/utils/request.ts'",
  schemaPath: 'http://localhost:8333/api/v3/api-docs',
  serversPath: './src',
})
