/**
 * OpenCode Git Plugin with Embedded HTTP Server
 * 
 * This plugin provides Git operations and auto-starts an HTTP server on port 4097.
 * 
 * DEBUGGING: Run OpenCode with environment variable DEBUG=git-plugin to see logs
 */

import simpleGit from 'simple-git';
import { tool } from '@opencode-ai/plugin';
import process from 'node:process';
import http from 'node:http';

const DEBUG = process.env.DEBUG?.includes('git-plugin');

// Track server state
let serverInstance = null;
let serverGit = null;

// ============================================
// EMBEDDED HTTP SERVER
// ============================================

/**
 * Create a simple HTTP server that handles Git operations
 */
function createHttpServer(git, workDir) {
    if (DEBUG) console.log('[Git Plugin] Creating HTTP server...');
    
    return http.createServer(async (req, res) => {
        // CORS headers
        res.setHeader('Access-Control-Allow-Origin', '*');
        res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
        res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
        
        if (req.method === 'OPTIONS') {
            res.writeHead(200);
            res.end();
            return;
        }

        if (DEBUG) console.log(`[Git Plugin] Request: ${req.method} ${req.url}`);
        
        // Parse URL - strip /git prefix if present
        let pathname = req.url.split('?')[0];
        if (pathname.startsWith('/git')) {
            pathname = pathname.slice(4) || '/';
        }
        
        const query = Object.fromEntries(new URL(req.url, 'http://localhost').searchParams);
        
        try {
            let body = '';
            if (req.method === 'POST') {
                body = await new Promise((resolve, reject) => {
                    let data = '';
                    req.on('data', chunk => data += chunk);
                    req.on('end', () => resolve(data));
                    req.on('error', reject);
                });
            }
            const jsonBody = body ? JSON.parse(body) : {};
            
            const result = await handleRoute(pathname, req.method, query, jsonBody, git, workDir);
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(result));
        } catch (error) {
            console.error(`[Git Plugin] Error handling ${pathname}:`, error.message);
            res.writeHead(500, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ success: false, error: error.message }));
        }
    });
}

async function handleRoute(pathname, method, query, body, git, workDir) {
    if (DEBUG) console.log(`[Git Plugin] Route: ${pathname}`);
    
    // Health check
    if (pathname === '/health' || pathname === '/') {
        return { healthy: true, workDir, version: '2.1.0-embedded' };
    }
    
    // GET /status
    if (pathname === '/status' && method === 'GET') {
        const status = await git.status();
        return {
            branch: status.current,
            upstream: status.tracking || null,
            ahead: status.ahead,
            behind: status.behind,
            staged: status.staged.map(path => ({ path, status: 'staged' })),
            unstaged: status.modified.map(path => ({ path, status: 'modified' })),
            untracked: status.not_added,
            conflicted: status.conflicted,
            deleted: status.deleted,
            renamed: status.renamed,
            clean: status.isClean()
        };
    }
    
    // GET /log
    if (pathname === '/log' && method === 'GET') {
        const limit = parseInt(query.limit) || 50;
        const skip = parseInt(query.skip) || 0;
        const branch = query.branch;
        
        const options = { '--max-count': limit, '--skip': skip };
        if (branch) options[branch] = null;
        
        const log = await git.log(options);
        return {
            commits: log.all.map(c => ({
                hash: c.hash,
                shortHash: c.hash.substring(0, 7),
                message: c.message,
                author: c.author_name,
                email: c.author_email,
                date: new Date(c.date).getTime()
            })),
            total: log.total,
            hasMore: log.all.length === limit
        };
    }
    
    // GET /branches
    if (pathname === '/branches' && method === 'GET') {
        const branches = await git.branch(['-a']);
        return {
            current: branches.current,
            branches: Object.entries(branches.branches).map(([name, info]) => ({
                name,
                current: info.current,
                remote: name.startsWith('remotes/'),
                commit: info.commit,
                label: info.label
            }))
        };
    }
    
    // GET /remotes
    if (pathname === '/remotes' && method === 'GET') {
        const remotes = await git.getRemotes(true);
        return remotes.map(r => ({
            name: r.name,
            fetchUrl: r.refs.fetch,
            pushUrl: r.refs.push
        }));
    }
    
    // GET /diff
    if (pathname === '/diff' && method === 'GET') {
        const cached = query.cached === 'true';
        const path = query.path;
        
        const args = cached ? ['--cached'] : [];
        if (path) args.push('--', path);
        
        const diff = await git.diff(args);
        const diffStat = await git.diffSummary(args);
        
        return {
            raw: diff,
            files: diffStat.files.map(f => ({
                file: f.file,
                additions: f.insertions,
                deletions: f.deletions,
                binary: f.binary
            })),
            additions: diffStat.insertions,
            deletions: diffStat.deletions
        };
    }
    
    // GET /stash
    if (pathname === '/stash' && method === 'GET') {
        const stashList = await git.stashList();
        return stashList.all.map((s, i) => ({
            index: i,
            hash: s.hash,
            message: s.message,
            date: s.date
        }));
    }
    
    // POST /stage
    if (pathname === '/stage' && method === 'POST') {
        const { files } = body;
        if (!files || files.length === 0) {
            await git.add('-A');
        } else {
            await git.add(files);
        }
        return { success: true, message: 'Files staged successfully' };
    }
    
    // POST /unstage
    if (pathname === '/unstage' && method === 'POST') {
        const { files } = body;
        if (!files || files.length === 0) {
            await git.reset(['HEAD']);
        } else {
            await git.reset(['HEAD', '--', ...files]);
        }
        return { success: true, message: 'Files unstaged successfully' };
    }
    
    // POST /discard
    if (pathname === '/discard' && method === 'POST') {
        const { files } = body;
        if (!files || files.length === 0) {
            return { success: false, error: 'Files required' };
        }
        await git.checkout(['--', ...files]);
        return { success: true, message: 'Changes discarded' };
    }
    
    // POST /commit
    if (pathname === '/commit' && method === 'POST') {
        const { message, amend, stageAll } = body;
        if (!message && !amend) {
            return { success: false, error: 'Commit message required' };
        }
        
        if (stageAll) {
            await git.add('-A');
        }
        
        const options = amend ? { '--amend': null } : undefined;
        const result = await git.commit(message, undefined, options);
        
        return {
            success: true,
            message: `Committed: ${result.commit}`,
            commit: result.commit,
            summary: result.summary
        };
    }
    
    // POST /push
    if (pathname === '/push' && method === 'POST') {
        const { remote = 'origin', branch, force, setUpstream } = body;
        const options = [];
        if (force) options.push('--force');
        if (setUpstream) options.push('-u');
        
        await git.push(remote, branch, options);
        return { success: true, message: 'Push successful' };
    }
    
    // POST /pull
    if (pathname === '/pull' && method === 'POST') {
        const { remote = 'origin', branch, rebase } = body;
        const options = rebase ? { '--rebase': null } : {};
        
        const result = await git.pull(remote, branch, options);
        return { success: true, message: 'Pull successful', summary: result.summary };
    }
    
    // POST /fetch
    if (pathname === '/fetch' && method === 'POST') {
        const { remote = 'origin', prune, all } = body;
        const options = [];
        if (prune) options.push('--prune');
        if (all) options.push('--all');
        
        await git.fetch(remote, undefined, options);
        return { success: true, message: 'Fetch successful' };
    }
    
    // POST /checkout
    if (pathname === '/checkout' && method === 'POST') {
        const { ref, create, force } = body;
        if (!ref) {
            return { success: false, error: 'Branch/ref required' };
        }
        
        const options = [];
        if (create) options.push('-b');
        if (force) options.push('-f');
        options.push(ref);
        
        await git.checkout(options);
        return { success: true, message: `Checked out ${ref}` };
    }
    
    // POST /stash/save
    if (pathname === '/stash/save' && method === 'POST') {
        const { message } = body;
        if (message) {
            await git.stash(['save', message]);
        } else {
            await git.stash();
        }
        return { success: true, message: 'Stash saved' };
    }
    
    // POST /stash/pop
    if (pathname === '/stash/pop' && method === 'POST') {
        const { index = 0 } = body;
        await git.stash(['pop', `stash@{${index}}`]);
        return { success: true, message: 'Stash applied and removed' };
    }
    
    if (DEBUG) console.log(`[Git Plugin] Unknown route: ${method} ${pathname}`);
    return { success: false, error: `Unknown route: ${method} ${pathname}` };
}

/**
 * Check if server is already running
 */
async function isServerRunning(port) {
    return new Promise(resolve => {
        const req = http.get(`http://127.0.0.1:${port}/health`, res => {
            resolve(res.statusCode === 200);
        });
        req.on('error', () => resolve(false));
        req.setTimeout(1000, () => {
            req.destroy();
            resolve(false);
        });
    });
}

/**
 * Start the embedded HTTP server
 */
async function startEmbeddedServer(workDir, port = 4097) {
    if (DEBUG) console.log('[Git Plugin] Attempting to start server...');
    
    // Already running check
    if (await isServerRunning(port)) {
        console.log(`[Git Plugin] ✓ Server already running on port ${port}`);
        return true;
    }
    
    // Don't start twice
    if (serverInstance) {
        console.log(`[Git Plugin] Server instance already exists`);
        return true;
    }
    
    serverGit = simpleGit(workDir);
    serverInstance = createHttpServer(serverGit, workDir);
    
    return new Promise((resolve, reject) => {
        serverInstance.on('error', (err) => {
            if (err.code === 'EADDRINUSE') {
                console.log(`[Git Plugin] Port ${port} in use, assuming server running`);
                resolve(true);
            } else {
                console.error(`[Git Plugin] Server error:`, err.message);
                reject(err);
            }
        });
        
        serverInstance.listen(port, '0.0.0.0', () => {
            console.log(`[Git Plugin] ✓ HTTP server started on http://0.0.0.0:${port}`);
            console.log(`[Git Plugin] Working directory: ${workDir}`);
            console.log(`[Git Plugin] Routes: /git/* and /*`);
            console.log(`[Git Plugin] DEBUG=${DEBUG}`);
            resolve(true);
        });
    });
}

// ============================================
// STATUS MAPPING
// ============================================

function mapStatus(status) {
    return {
        branch: status.current,
        ahead: status.ahead,
        behind: status.behind,
        staged: status.files.filter(f => f.index !== ' ' && f.index !== '?').map(f => ({
            path: f.path,
            status: mapFileStatus(f.index),
            oldPath: null
        })),
        unstaged: status.files.filter(f => f.working_dir !== ' ' && f.working_dir !== '?').map(f => ({
            path: f.path,
            status: mapFileStatus(f.working_dir)
        })),
        untracked: status.not_added,
        clean: status.isClean()
    };
}

function mapFileStatus(code) {
    switch (code) {
        case 'A': return 'added';
        case 'M': return 'modified';
        case 'D': return 'deleted';
        case 'R': return 'renamed';
        case '?': return 'unknown';
        default: return 'modified';
    }
}

// ============================================
// MAIN PLUGIN
// ============================================

/**
 * Main plugin function - starts HTTP server IMMEDIATELY on load
 */
const GitPlugin = async (context) => {
    console.log('='.repeat(60));
    console.log('[Git Plugin] ============================================');
    console.log('[Git Plugin] OPENCODE GIT PLUGIN INITIALIZING...');
    console.log('[Git Plugin] ============================================');
    console.log('='.repeat(60));
    
    const workDir = context?.directory || process.cwd();
    const git = simpleGit(workDir);
    
    console.log(`[Git Plugin] Working directory: ${workDir}`);
    console.log(`[Git Plugin] Node.js version: ${process.version}`);
    console.log(`[Git Plugin] DEBUG=${DEBUG}`);
    
    // START HTTP SERVER IMMEDIATELY (before returning hooks)
    try {
        console.log('[Git Plugin] Starting embedded HTTP server...');
        const started = await startEmbeddedServer(workDir, 4097);
        if (started) {
            console.log('[Git Plugin] ✓ Server started successfully');
        } else {
            console.log('[Git Plugin] ⚠ Server start returned false');
        }
    } catch (err) {
        console.error('[Git Plugin] ✗ Failed to start HTTP server:', err.message);
        console.error(err.stack);
        // Continue anyway - tools will still work for AI agent
    }
    
    console.log('[Git Plugin] Providing Git tools to AI agent...');
    
    return {
        // ==========================================
        // AI TOOLS (for OpenCode agent use)
        // ==========================================
        
        tool: {
            git_status: tool({
                description: 'Get the current git status of the workspace',
                args: {},
                execute: async () => {
                    const status = await git.status();
                    return mapStatus(status);
                }
            }),

            git_log: tool({
                description: 'Get the git commit log',
                args: {
                    limit: tool.schema.number().optional().default(50),
                    skip: tool.schema.number().optional().default(0),
                    branch: tool.schema.string().optional()
                },
                execute: async ({ limit, skip, branch }) => {
                    const options = { '--max-count': limit, '--skip': skip };
                    if (branch) options[branch] = null;
                    
                    const log = await git.log(options);
                    return {
                        commits: log.all.map(c => ({
                            hash: c.hash,
                            shortHash: c.hash.substring(0, 7),
                            message: c.message,
                            author: c.author_name,
                            email: c.author_email,
                            date: new Date(c.date).getTime(),
                            parents: [],
                            refs: []
                        })),
                        total: log.total
                    };
                }
            }),

            git_diff: tool({
                description: 'Get the git diff',
                args: {
                    path: tool.schema.string().optional(),
                    cached: tool.schema.boolean().optional().default(false)
                },
                execute: async ({ path, cached }) => {
                    const args = [];
                    if (cached) args.push('--cached');
                    if (path) args.push(path);
                    
                    const rawDiff = await git.diff(args);
                    return {
                        files: [{
                            path: path || "diff",
                            status: "modified",
                            hunks: [{
                                oldStart: 0, oldLines: 0, newStart: 0, newLines: 0,
                                header: "Raw Diff",
                                lines: rawDiff.split('\n').map(line => {
                                    let type = "context";
                                    if (line.startsWith('+')) type = "addition";
                                    if (line.startsWith('-')) type = "deletion";
                                    return { type, content: line };
                                })
                            }]
                        }]
                    };
                }
            }),

            git_stage: tool({
                description: 'Stage files for commit',
                args: {
                    files: tool.schema.array(tool.schema.string())
                },
                execute: async ({ files }) => {
                    await git.add(files);
                    return { success: true, message: `Staged ${files.length} files` };
                }
            }),

            git_unstage: tool({
                description: 'Unstage files',
                args: {
                    files: tool.schema.array(tool.schema.string())
                },
                execute: async ({ files }) => {
                    await git.reset(['HEAD', ...files]);
                    return { success: true, message: `Unstaged ${files.length} files` };
                }
            }),

            git_commit: tool({
                description: 'Commit staged changes',
                args: {
                    message: tool.schema.string(),
                    files: tool.schema.array(tool.schema.string()).optional(),
                    amend: tool.schema.boolean().optional()
                },
                execute: async ({ message, files, amend }) => {
                    const options = amend ? { '--amend': null } : {};
                    await git.commit(message, files || [], options);
                    return { success: true, message: "Commit successful" };
                }
            }),

            git_push: tool({
                description: 'Push changes to remote',
                args: {
                    remote: tool.schema.string().optional().default('origin'),
                    branch: tool.schema.string().optional(),
                    force: tool.schema.boolean().optional(),
                    setUpstream: tool.schema.boolean().optional()
                },
                execute: async ({ remote, branch, force, setUpstream }) => {
                    const options = {};
                    if (force) options['--force'] = null;
                    if (setUpstream) options['-u'] = null;
                    
                    await git.push(remote, branch, options);
                    return { success: true, message: "Push successful" };
                }
            }),

            git_pull: tool({
                description: 'Pull changes from remote',
                args: {
                    remote: tool.schema.string().optional().default('origin'),
                    branch: tool.schema.string().optional(),
                    rebase: tool.schema.boolean().optional()
                },
                execute: async ({ remote, branch, rebase }) => {
                    const options = rebase ? { '--rebase': null } : {};
                    await git.pull(remote, branch, options);
                    return { success: true, message: "Pull successful" };
                }
            }),

            git_fetch: tool({
                description: 'Fetch changes from remote',
                args: {
                    remote: tool.schema.string().optional().default('origin'),
                    prune: tool.schema.boolean().optional(),
                    all: tool.schema.boolean().optional()
                },
                execute: async ({ remote, prune, all }) => {
                    const options = {};
                    if (prune) options['--prune'] = null;
                    if (all) options['--all'] = null;
                    
                    await git.fetch(remote, options);
                    return { success: true, message: "Fetch successful" };
                }
            }),

            git_checkout: tool({
                description: 'Checkout a branch or commit',
                args: {
                    ref: tool.schema.string(),
                    create: tool.schema.boolean().optional(),
                    force: tool.schema.boolean().optional()
                },
                execute: async ({ ref, create, force }) => {
                    const options = {};
                    if (create) options['-b'] = null;
                    if (force) options['-f'] = null;
                    
                    await git.checkout(ref, options);
                    return { success: true, message: `Checked out ${ref}` };
                }
            }),

            git_branches: tool({
                description: 'List local branches',
                args: {},
                execute: async () => {
                    const branchSummary = await git.branchLocal();
                    return branchSummary.all.map(b => ({
                        name: b,
                        current: b === branchSummary.current,
                        remote: false,
                        ahead: 0,
                        behind: 0
                    }));
                }
            }),

            git_remotes: tool({
                description: 'List git remotes',
                args: {},
                execute: async () => {
                    const remotes = await git.getRemotes(true);
                    return remotes.map(r => ({
                        name: r.name,
                        url: r.refs.fetch || r.refs.push,
                        fetchUrl: r.refs.fetch,
                        pushUrl: r.refs.push
                    }));
                }
            }),

            git_stashes: tool({
                description: 'List git stashes',
                args: {},
                execute: async () => {
                    const stashList = await git.stashList();
                    return stashList.all.map((s, index) => ({
                        index,
                        message: s.message,
                        date: new Date(s.date).getTime()
                    }));
                }
            })
        }
    };
}

// ============================================
// STANDALONE EXECUTION
// ============================================

// Allow running standalone via "bun run git-plugin.js start-server"
if (process.argv.includes('start-server')) {
    console.log('[Git Plugin] Standalone mode detected');
    const workDir = process.cwd();
    startEmbeddedServer(workDir, 4097).then(success => {
        if (!success) process.exit(1);
        // Keep process alive
        setInterval(() => {}, 1000 * 60 * 60);
    }).catch(err => {
        console.error(err);
        process.exit(1);
    });
}

export { GitPlugin };
export default GitPlugin;
