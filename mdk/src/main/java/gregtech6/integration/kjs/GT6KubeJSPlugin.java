/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */
//? if kjs {
package gregtech6.integration.kjs;

/**
 * THE dual-leg fork of the KJS module (task p34-kjs-bindings): the one file in the
 * package importing KubeJS types. The 6.x/7.x divergence is the WHOLE hook surface,
 * not just the class declaration (the original card spec ② underestimated it):
 * <ul>
 * <li>1.20.1 forge, KubeJS 6 (2001.6.5-build.16): {@code KubeJSPlugin} is a CLASS
 *     (dev.latvian.mods.kubejs.KubeJSPlugin.java:29) — {@code extends}; bindings via
 *     {@code registerBindings(BindingsEvent)} (:54, BindingsEvent.add(String,Object)
 *     :18); classes via {@code registerClasses(ScriptType, ClassFilter)} (:51).</li>
 * <li>1.21.1 neoforge, KubeJS 7 (2101.7.2-build.336): {@code KubeJSPlugin} is an
 *     INTERFACE (dev.latvian.mods.kubejs.plugin.KubeJSPlugin.java:60) —
 *     {@code implements}; bindings via {@code registerBindings(BindingRegistry)}
 *     (:93, BindingRegistry.add(String,Object) :10); classes via
 *     {@code registerClasses(ClassFilter)} (:89, no ScriptType).</li>
 * </ul>
 * Everything else delegates to the KubeJS-free {@link GT6KJS} core. Discovered via
 * the resource-root {@code kubejs.plugins.txt} (bare FQCN lines — the GTCEu
 * kubejs.plugins.txt form; KubeJSPlugins.loadFromFile parses FQCN [+client] [#comment]).
 * Optional-dependency semantics: KubeJS absent = nobody reads plugins.txt = this
 * class never loads = zero classpath contagion.
 *
 * <p>Chisel shape note: the forge branch stays PLAIN and the neo branch is the
 * block-commented else-leg — the at-rest raw source must be valid Java for the
 * ACTIVE node's in-place compile (the GT6RecipeMaps.RegistrationFreezer :294-310
 * convention; both-branches-commented produced an empty compilation unit, caught by
 * the class-file census before any commit).
 */
//? if forge {
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;

public class GT6KubeJSPlugin extends KubeJSPlugin {

	@Override
	public void registerBindings(BindingsEvent aEvent) {
		GT6KJS.bindingClasses().forEach(aEvent::add);
	}

	@Override
	public void registerClasses(ScriptType aType, ClassFilter aFilter) {
		for (String tPrefix : GT6KJS.classFilterPrefixes()) aFilter.allow(tPrefix);
	}
}
//?} else {
/*import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public class GT6KubeJSPlugin implements KubeJSPlugin {

	@Override
	public void registerBindings(BindingRegistry aBindings) {
		GT6KJS.bindingClasses().forEach(aBindings::add);
	}

	@Override
	public void registerClasses(ClassFilter aFilter) {
		for (String tPrefix : GT6KJS.classFilterPrefixes()) aFilter.allow(tPrefix);
	}
}
*///?}
//?}
