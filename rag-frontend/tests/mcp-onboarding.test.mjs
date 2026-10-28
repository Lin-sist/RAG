import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'
import { readFile } from 'node:fs/promises'
import { createSSRApp } from 'vue'
import { renderToString } from 'vue/server-renderer'
const server = await createServer({ configFile:false, plugins:[vue()], optimizeDeps:{noDiscovery:true,include:[]}, server:{middlewareMode:true,hmr:{server:createHttpServer()}},resolve:{alias:{'@':fileURLToPath(new URL('../src',import.meta.url))}}, appType:'custom' })
try {
  const { normalizeMcpEndpoint, buildMcpClientSetup, MCP_DEFAULT_ENDPOINT, MCP_DISCOVERY_PROMPT, MCP_QUERY_PROMPT } = await server.ssrLoadModule('/src/utils/mcpOnboarding.ts')
  await test('accepted endpoints are normalized without probing or replacing the fixed browser check path',()=>{
    for(const [raw,expected] of [[' HTTP://LOCALHOST:8080/mcp ','http://localhost:8080/mcp'],['http://[::1]:8080/mcp','http://[::1]:8080/mcp'],['https://kb.example.test/rag/mcp','https://kb.example.test/rag/mcp']]) assert.equal(normalizeMcpEndpoint(raw),expected)
  })
  await test('credentials, query/fragment and shell payloads cannot become command or install links',()=>{
    for(const address of ['javascript:alert(1)','file:///mcp','https://user:token@host/mcp','http://@host/mcp','https://host/mcp?token=x','https://host/mcp?','https://host/mcp#','https://host/mcp#x','http://host/MCP','http://host/mcp/','http://host/mcp\n','http://host/$(touch)/mcp','http://host/`whoami`/mcp','http://host/a;cmd/mcp','http://host/a&cmd/mcp','http://host/a"cmd/mcp','http://host/%0A/mcp','https://host/not-mcp','x'.repeat(513)]){
      assert.equal(normalizeMcpEndpoint(address),null,address)
      for(const client of ['codex','claude-code','cursor'])assert.throws(()=>buildMcpClientSetup(client,address),/MCP_ENDPOINT_INVALID/)
    }
  })
  await test('Codex command and TOML use a dedicated environment reference, not a copied secret',()=>{
    const setup=buildMcpClientSetup('codex',MCP_DEFAULT_ENDPOINT)
    assert.equal(setup.command,'codex mcp add enterprise-rag --url "http://127.0.0.1:8080/mcp" --bearer-token-env-var RAG_MCP_ACCESS_TOKEN')
    assert.match(setup.config,/\[mcp_servers.enterprise-rag\]/);assert.match(setup.config,/bearer_token_env_var = "RAG_MCP_ACCESS_TOKEN"/)
    assert.equal(setup.installUrl,null)
  })
  await test('Claude Code exports HTTP project configuration with client-side variable expansion',()=>{
    const setup=buildMcpClientSetup('claude-code','https://kb.example.test/rag/mcp')
    assert.deepEqual(JSON.parse(setup.config),{mcpServers:{'enterprise-rag':{type:'http',url:'https://kb.example.test/rag/mcp',headers:{Authorization:'Bearer ${RAG_MCP_ACCESS_TOKEN}'}}}})
    assert.equal(setup.command,null);assert.equal(setup.installUrl,null)
  })
  await test('Cursor deeplink decodes to exactly the same HTTP configuration as its manual fallback',()=>{
    const setup=buildMcpClientSetup('cursor',MCP_DEFAULT_ENDPOINT),link=new URL(setup.installUrl)
    assert.equal(link.protocol,'cursor:');assert.equal(link.hostname,'anysphere.cursor-deeplink');assert.equal(link.pathname,'/mcp/install')
    assert.deepEqual([...link.searchParams.keys()],['name','config']);assert.equal(link.searchParams.get('name'),'enterprise-rag')
    const decoded=JSON.parse(Buffer.from(link.searchParams.get('config'),'base64').toString('utf8'))
    assert.deepEqual(decoded,JSON.parse(setup.config).mcpServers['enterprise-rag'])
    assert.deepEqual(decoded,{url:MCP_DEFAULT_ENDPOINT,headers:{Authorization:'Bearer ${env:RAG_MCP_ACCESS_TOKEN}'}})
  })
  await test('changing endpoints regenerates every client payload without changing MCP protocol adapter',async()=>{
    for(const client of ['codex','claude-code','cursor']){
      const setup=buildMcpClientSetup(client,'https://other.example.test/mcp')
      assert.match(setup.config,/other.example.test/);assert.ok(!setup.config.includes('127.0.0.1'))
    }
    const adapter=await readFile(new URL('../src/utils/mcpCheck.ts',import.meta.url),'utf8')
    assert.match(adapter,/transport\('\/mcp'/);assert.doesNotMatch(adapter,/mcpOnboarding|normalizeMcpEndpoint/)
    assert.match(MCP_DISCOVERY_PROMPT,/暂时不要调用/);assert.match(MCP_QUERY_PROMPT,/引用出处/)
  })
  await test('client dialog initially presents named logo choices and no installed/connected claims',async()=>{
    const {default:Dialog}=await server.ssrLoadModule('/src/components/mcp/McpConnectDialog.vue')
    const context={};await renderToString(createSSRApp(Dialog,{metadata:null,metadataState:'error'}),context)
    const html=context.teleports.body
    assert.match(html,/role="dialog"/);assert.match(html,/aria-modal="true"/)
    for(const client of ['Codex','Claude Code','Cursor'])assert.ok(html.includes('选择 '+client))
    assert.equal((html.match(/aria-pressed="false"/g)||[]).length,3)
    assert.doesNotMatch(html,/已安装|已连接|扫描本机|<input/)
    assert.equal((html.match(/<img /g)||[]).length,3)
  })
  await test('brand assets remain local inert SVGs with the upstream license included',async()=>{
    for(const name of ['codex-color.svg','claudecode-color.svg','cursor.svg']){
      const svg=await readFile(new URL('../src/assets/mcp-clients/'+name,import.meta.url),'utf8')
      assert.match(svg,/<svg/);assert.doesNotMatch(svg,/<script|<foreignObject|\bon[a-z]+\s*=|href\s*=\s*["']https?:/i)
    }
    assert.match(await readFile(new URL('../src/assets/mcp-clients/LICENSE.lobehub',import.meta.url),'utf8'),/MIT License/)
  })
} finally {await server.close()}
