package com.opendesign.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

data class HyperFrame(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val animationType: String,
    val fps: Int = 30,
    val durationMs: Int = 3000,
    val htmlTemplate: String
)

private val HYPERFRAME_TEMPLATES = listOf(
    HyperFrame(
        id = "fade-in",
        name = "Fade In",
        description = "Elements fade in from transparent",
        category = "Transition",
        animationType = "opacity",
        htmlTemplate = """<!DOCTYPE html><html><head><style>
body{margin:0;display:flex;align-items:center;justify-content:center;height:100vh;background:#0a0a0a;font-family:system-ui}
.container{text-align:center}
.title{font-size:48px;font-weight:800;color:white;opacity:0;animation:fadeIn 1.5s ease forwards}
.subtitle{font-size:24px;color:#888;opacity:0;animation:fadeIn 1.5s ease 0.5s forwards}
@keyframes fadeIn{to{opacity:1}}
</style></head><body><div class="container"><div class="title">Welcome</div><div class="subtitle">Motion Graphics</div></div></body></html>"""
    ),
    HyperFrame(
        id = "slide-up",
        name = "Slide Up",
        description = "Elements slide up into view",
        category = "Transition",
        animationType = "transform",
        htmlTemplate = """<!DOCTYPE html><html><head><style>
body{margin:0;display:flex;align-items:center;justify-content:center;height:100vh;background:#0a0a0a;font-family:system-ui}
.container{text-align:center}
.title{font-size:48px;font-weight:800;color:white;transform:translateY(100px);opacity:0;animation:slideUp 1s ease forwards}
.subtitle{font-size:24px;color:#888;transform:translateY(50px);opacity:0;animation:slideUp 1s ease 0.3s forwards}
@keyframes slideUp{to{transform:translateY(0);opacity:1}}
</style></head><body><div class="container"><div class="title">Slide Up</div><div class="subtitle">Animated Content</div></div></body></html>"""
    ),
    HyperFrame(
        id = "scale-pop",
        name = "Scale Pop",
        description = "Elements pop in with scaling effect",
        category = "Transition",
        animationType = "scale",
        htmlTemplate = """<!DOCTYPE html><html><head><style>
body{margin:0;display:flex;align-items:center;justify-content:center;height:100vh;background:#0a0a0a;font-family:system-ui}
.container{text-align:center}
.title{font-size:48px;font-weight:800;color:white;transform:scale(0);animation:popIn 0.8s cubic-bezier(0.68,-0.55,0.27,1.55) forwards}
@keyframes popIn{to{transform:scale(1)}}
.card{width:300px;height:200px;background:linear-gradient(135deg,#667eea,#764ba2);border-radius:16px;transform:scale(0);animation:popIn 0.8s cubic-bezier(0.68,-0.55,0.27,1.55) 0.3s forwards;margin:20px auto}
</style></head><body><div class="container"><div class="title">Pop!</div><div class="card"></div></div></body></html>"""
    ),
    HyperFrame(
        id = "typewriter",
        name = "Typewriter",
        description = "Text types out character by character",
        category = "Text",
        animationType = "text",
        htmlTemplate = """<!DOCTYPE html><html><head><style>
body{margin:0;display:flex;align-items:center;justify-content:center;height:100vh;background:#0a0a0a;font-family:monospace}
.title{font-size:36px;color:#00ff88;overflow:hidden;border-right:3px solid #00ff88;white-space:nowrap;animation:typing 3s steps(20) forwards,blink 0.5s step-end infinite}
@keyframes typing{from{width:0}to{width:100%}}
@keyframes blink{50%{border-color:transparent}}
</style></head><body><div class="title">Hello, World!</div></body></html>"""
    ),
    HyperFrame(
        id = "parallax",
        name = "Parallax Scroll",
        description = "Multi-layer parallax depth effect",
        category = "Depth",
        animationType = "parallax",
        htmlTemplate = """<!DOCTYPE html><html><head><style>
body{margin:0;height:200vh;background:#0a0a0a;font-family:system-ui;overflow-x:hidden}
.layer{position:fixed;top:0;left:0;width:100%;height:100vh;display:flex;align-items:center;justify-content:center}
.bg{font-size:120px;font-weight:900;color:rgba(100,126,234,0.1);transform:translateY(calc(var(--scroll) * 0.1px))}
.mid{font-size:60px;font-weight:700;color:rgba(118,75,162,0.3);transform:translateY(calc(var(--scroll) * 0.3px))}
.fg{font-size:36px;color:white;transform:translateY(calc(var(--scroll) * 0.5px))}
</style></head><body>
<div class="layer bg" style="--scroll:0">DEPTH</div>
<div class="layer mid" style="--scroll:100">PARALLAX</div>
<div class="layer fg" style="--scroll:200">Scroll down</div>
<script>window.addEventListener('scroll',()=>{document.documentElement.style.setProperty('--scroll',window.scrollY)})</script>
</body></html>"""
    ),
    HyperFrame(
        id = "glitch",
        name = "Glitch Text",
        description = "Glitch/distortion text animation",
        category = "Text",
        animationType = "glitch",
        htmlTemplate = """<!DOCTYPE html><html><head><style>
body{margin:0;display:flex;align-items:center;justify-content:center;height:100vh;background:#0a0a0a;font-family:system-ui}
.title{font-size:72px;font-weight:900;color:white;position:relative;animation:glitch 2s infinite}
.title::before,.title::after{content:attr(data-text);position:absolute;top:0;left:0;width:100%;height:100%}
.title::before{color:#ff0080;animation:glitch1 0.3s infinite;clip-path:polygon(0 0,100% 0,100% 35%,0 35%)}
.title::after{color:#00ffff;animation:glitch2 0.3s infinite;clip-path:polygon(0 65%,100% 65%,100% 100%,0 100%)}
@keyframes glitch1{0%,100%{transform:translate(0)}20%{transform:translate(-3px,3px)}40%{transform:translate(3px,-3px)}60%{transform:translate(-3px,0)}80%{transform:translate(3px,3px)}}
@keyframes glitch2{0%,100%{transform:translate(0)}20%{transform:translate(3px,-3px)}40%{transform:translate(-3px,3px)}60%{transform:translate(3px,0)}80%{transform:translate(-3px,-3px)}}
@keyframes glitch{0%,90%,100%{opacity:1}91%{opacity:0.8}92%{opacity:1}93%{opacity:0.6}94%{opacity:1}}
</style></head><body><div class="title" data-text="GLITCH">GLITCH</div></body></html>"""
    ),
    HyperFrame(
        id = "particle-burst",
        name = "Particle Burst",
        description = "Explosive particle animation",
        category = "Effect",
        animationType = "particles",
        htmlTemplate = """<!DOCTYPE html><html><head><style>
body{margin:0;display:flex;align-items:center;justify-content:center;height:100vh;background:#0a0a0a;overflow:hidden}
.particles{position:relative;width:100%;height:100%}
.p{position:absolute;width:8px;height:8px;border-radius:50%;top:50%;left:50%;animation:burst 2s ease-out forwards}
@keyframes burst{0%{transform:translate(0,0) scale(1);opacity:1}100%{transform:translate(var(--tx),var(--ty)) scale(0);opacity:0}}
</style></head><body><div class="particles" id="p"></div>
<script>const c=document.getElementById('p');for(let i=0;i<50;i++){const p=document.createElement('div');p.className='p';const a=Math.random()*Math.PI*2;const d=100+Math.random()*300;p.style.cssText='--tx:'+Math.cos(a)*d+'px;--ty:'+Math.sin(a)*d+'px;background:hsl('+Math.random()*360+',100%,60%);animation-delay:'+Math.random()*0.5+'s';c.appendChild(p)}</script>
</body></html>"""
    ),
    HyperFrame(
        id = "wave-morph",
        name = "Wave Morph",
        description = "Fluid wave morphing animation",
        category = "Effect",
        animationType = "morph",
        htmlTemplate = """<!DOCTYPE html><html><head><style>
body{margin:0;height:100vh;background:#0a0a0a;overflow:hidden}
svg{position:fixed;bottom:0;width:100%;height:50vh}
.wave{animation:morph 8s ease-in-out infinite alternate}
@keyframes morph{0%{d:path('M0,160 C320,300,420,50,640,160 C880,280,960,100,1280,160 L1280,320 L0,320 Z')}100%{d:path('M0,200 C200,80,400,280,640,120 C880,0,1060,240,1280,200 L1280,320 L0,320 Z')}}
</style></head><body>
<svg viewBox="0 0 1280 320"><path class="wave" fill="rgba(100,126,234,0.3)" d="M0,160 C320,300,420,50,640,160 C880,280,960,100,1280,160 L1280,320 L0,320 Z"/></svg>
<svg viewBox="0 0 1280 320" style="animation-delay:2s"><path class="wave" fill="rgba(118,75,162,0.3)" d="M0,200 C200,80,400,280,640,120 C880,0,1060,240,1280,200 L1280,320 L0,320 Z"/></svg>
</body></html>"""
    ),
    HyperFrame(
        id = "counter",
        name = "Number Counter",
        description = "Animated number counting up",
        category = "Data",
        animationType = "counter",
        htmlTemplate = """<!DOCTYPE html><html><head><style>
body{margin:0;display:flex;align-items:center;justify-content:center;height:100vh;background:#0a0a0a;font-family:system-ui}
.counter{font-size:96px;font-weight:900;color:white;font-variant-numeric:tabular-nums}
.label{font-size:24px;color:#888;margin-top:8px}
</style></head><body><div style="text-align:center"><div class="counter" id="c">0</div><div class="label">users</div></div>
<script>let n=0;const t=10000;const s=Date.now();function u(){const e=Date.now()-s;const p=Math.min(e/t,1);const v=Math.floor(p*p*t/100);document.getElementById('c').textContent=v.toLocaleString();if(p<1)requestAnimationFrame(u)}u()</script>
</body></html>"""
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HyperFramesScreen(
    onFrameSelect: (HyperFrame) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Transition", "Text", "Depth", "Effect", "Data")
    val filtered = if (selectedCategory == "All") HYPERFRAME_TEMPLATES
        else HYPERFRAME_TEMPLATES.filter { it.category == selectedCategory }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "HyperFrames",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Motion graphics templates with code-native animations",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        items(filtered) { frame ->
            HyperFrameCard(frame) { onFrameSelect(frame) }
        }
    }
}

@Composable
fun HyperFrameCard(frame: HyperFrame, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Preview
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        loadDataWithBaseURL(null, frame.htmlTemplate, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = frame.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = frame.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AssistChip(
                        onClick = {},
                        label = { Text(frame.animationType, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    }
}
